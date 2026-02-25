package com.pos.service;

import com.pos.dto.configuracion.AdminConfigDTO;
import com.pos.dto.venta.VentaPagoDetalleDTO;
import com.pos.dto.venta.VentaResponseDTO;
import com.pos.entity.TipoVenta;
import com.pos.entity.Venta;
import com.pos.entity.VentaDetalle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class ImpresoraTtermicaService {
    private static final int PAPER_WIDTH = 48;
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int TRAILING_FEED_LINES = 8;

    private static final String LINE = "-".repeat(PAPER_WIDTH);
    private static final String DOUBLE_LINE = "=".repeat(PAPER_WIDTH);
    private final ConfiguracionService configuracionService;
    private final VentaPagoDetalleService ventaPagoDetalleService;

    public void imprimirFactura(Venta venta) {
        imprimirFactura(venta, null, null);
    }

    public void imprimirFactura(Venta venta, BigDecimal pagoEfectivo, BigDecimal pagoTransferencia) {
        StringBuilder sb = new StringBuilder();
        AdminConfigDTO cfg = configuracionService.obtener();
        BigDecimal efectivoFinal = pagoEfectivo;
        BigDecimal transferenciaFinal = pagoTransferencia;

        if (efectivoFinal == null && transferenciaFinal == null && venta != null && venta.getId() != null) {
            VentaPagoDetalleDTO detallePago = ventaPagoDetalleService.obtener(venta.getId());
            if (detallePago != null) {
                efectivoFinal = detallePago.pagoEfectivo();
                transferenciaFinal = detallePago.pagoTransferencia();
            }
        }

        appendCentered(sb, safe(cfg.negocioNombre()));
        if (!isBlank(cfg.negocioNit())) {
            appendWrappedLabel(sb, "NIT", cfg.negocioNit(), 0);
        }
        if (!isBlank(cfg.negocioTelefono())) {
            appendWrappedLabel(sb, "Tel", cfg.negocioTelefono(), 0);
        }
        if (!isBlank(cfg.negocioDireccion())) {
            appendWrappedLabel(sb, "Dir", cfg.negocioDireccion(), 0);
        }
        if (!isBlank(cfg.ticketEncabezado())) {
            appendCentered(sb, cfg.ticketEncabezado());
        }
        sb.append(DOUBLE_LINE).append("\n");
        appendCentered(sb, "FACTURA");
        sb.append(DOUBLE_LINE).append("\n");
        sb.append(kvLine("No", String.valueOf(venta.getId()))).append("\n");
        sb.append(kvLine("Fecha", formatFecha(venta.getFecha()))).append("\n");
        sb.append(kvLine("Tipo", valueOrDash(venta.getTipoVenta() == null ? null : venta.getTipoVenta().name()))).append("\n");
        sb.append(kvLine("Estado", valueOrDash(venta.getEstado() == null ? null : venta.getEstado().name()))).append("\n");
        appendWrappedLabel(sb, "Cliente", valueOrDash(venta.getClienteNombre()), 0);

        if (venta.getTipoVenta() == com.pos.entity.TipoVenta.DOMICILIO) {
            appendWrappedLabel(sb, "Telefono", valueOrDash(venta.getTelefono()), 0);
            appendWrappedLabel(sb, "Direccion", valueOrDash(venta.getDireccion()), 0);
        }

        sb.append(LINE).append("\n");

        BigDecimal subtotal = BigDecimal.ZERO;
        if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
            for (VentaDetalle detalle : venta.getDetalles()) {
                String nombreProducto = detalle.getProducto() != null
                        ? detalle.getProducto().getNombre()
                        : "Producto";

                String itemLabel = detalle.getCantidad() + " x " + safe(nombreProducto);
                String itemTotal = "$" + formatMoneda(detalle.getSubtotal());
                String itemLine = kvLine(itemLabel, itemTotal);
                if (itemLine.length() <= PAPER_WIDTH) {
                    sb.append(itemLine).append("\n");
                } else {
                    appendWrapped(sb, itemLabel, 0);
                    sb.append(kvLine("Total", itemTotal)).append("\n");
                }

                if (detalle.getObservacion() != null && !detalle.getObservacion().isBlank()) {
                    appendWrappedLabel(sb, "  Obs", detalle.getObservacion(), 0);
                }
                sb.append(LINE).append("\n");
                subtotal = subtotal.add(detalle.getSubtotal() != null ? detalle.getSubtotal() : BigDecimal.ZERO);
            }
        }

        sb.append(kvLine("SUBTOTAL", "$" + formatMoneda(subtotal))).append("\n");
        if (venta.getValorDomicilio() != null) {
            sb.append(kvLine("DOMICILIO", "$" + formatMoneda(venta.getValorDomicilio()))).append("\n");
        }
        if (venta.getDescuentoValor() != null && venta.getDescuentoValor().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(kvLine("DESCUENTO", "-$" + formatMoneda(venta.getDescuentoValor()))).append("\n");
        }
        sb.append(DOUBLE_LINE).append("\n");
        sb.append(kvLine("TOTAL", "$" + formatMoneda(venta.getTotal()))).append("\n");
        appendPagoFactura(sb, venta, efectivoFinal, transferenciaFinal);
        sb.append(DOUBLE_LINE).append("\n");
        appendCentered(sb, isBlank(cfg.ticketPie()) ? "Gracias por su compra" : cfg.ticketPie());
        appendTrailingFeed(sb);

        enviarAImpresora(sb.toString());
    }

    public void imprimirTicketVenta(Venta venta) {
        imprimirFactura(venta);
    }

    public void imprimirTicketCocina(Venta venta) {
        StringBuilder sb = new StringBuilder();

        appendCentered(sb, "TICKET COCINA");
        if (venta.getTipoVenta() == TipoVenta.DOMICILIO) {
            appendCentered(sb, "PEDIDO DOMICILIO");
        }
        sb.append(DOUBLE_LINE).append("\n");
        sb.append(kvLine("Pedido", String.valueOf(venta.getId()))).append("\n");
        sb.append(kvLine("Fecha", formatFecha(venta.getFecha()))).append("\n");
        appendWrappedLabel(sb, "Cliente", valueOrDash(venta.getClienteNombre()), 0);
        sb.append(LINE).append("\n");

        if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
            for (VentaDetalle detalle : venta.getDetalles()) {
                String nombreProducto = detalle.getProducto() != null
                        ? detalle.getProducto().getNombre()
                        : "Producto";

                appendWrapped(sb, "* " + detalle.getCantidad() + " x " + safe(nombreProducto), 0);

                if (detalle.getObservacion() != null && !detalle.getObservacion().isBlank()) {
                    appendWrappedLabel(sb, "   Obs", detalle.getObservacion(), 0);
                }
                sb.append(LINE).append("\n");
            }
        } else {
            appendCentered(sb, "SIN DETALLE DE PRODUCTOS");
            sb.append(LINE).append("\n");
        }

        appendCentered(sb, "ENVIAR A PREPARACION");
        sb.append(DOUBLE_LINE).append("\n");
        appendTrailingFeed(sb);
        enviarAImpresora(sb.toString());
    }

    // Fallback para flujos que solo tienen DTO resumido.
    public void imprimirTicketVenta(VentaResponseDTO venta) {
        StringBuilder sb = new StringBuilder();

        appendCentered(sb, "TICKET DE VENTA");
        sb.append(DOUBLE_LINE).append("\n");
        sb.append(kvLine("ID", String.valueOf(venta.id()))).append("\n");
        sb.append(kvLine("Fecha", valueOrDash(String.valueOf(venta.fecha())))).append("\n");
        sb.append(kvLine("Tipo", valueOrDash(venta.tipoVenta() == null ? null : venta.tipoVenta().name()))).append("\n");
        sb.append(kvLine("Estado", valueOrDash(venta.estado() == null ? null : venta.estado().name()))).append("\n");
        appendWrappedLabel(sb, "Cliente", valueOrDash(venta.clienteNombre()), 0);
        sb.append(DOUBLE_LINE).append("\n");
        sb.append(kvLine("TOTAL", "$" + formatMoneda(venta.total()))).append("\n");
        sb.append(kvLine("PAGO", valueOrDash(venta.formaPago() == null ? null : venta.formaPago().name()))).append("\n");
        sb.append(DOUBLE_LINE).append("\n");
        appendTrailingFeed(sb);

        enviarAImpresora(sb.toString());
    }

    private String formatMoneda(BigDecimal valor) {
        if (valor == null) {
            return "0";
        }
        BigDecimal scaled = valor.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return scaled.scale() < 0 ? scaled.setScale(0).toPlainString() : scaled.toPlainString();
    }

    private String formatFecha(LocalDateTime fecha) {
        if (fecha == null) {
            return "-";
        }
        return DATE_TIME_FMT.format(fecha);
    }

    private void appendCentered(StringBuilder sb, String text) {
        String t = safe(text);
        if (t.length() >= PAPER_WIDTH) {
            appendWrapped(sb, t, 0);
            return;
        }
        int leftPad = (PAPER_WIDTH - t.length()) / 2;
        sb.append(" ".repeat(Math.max(leftPad, 0))).append(t).append("\n");
    }

    private String kvLine(String key, String value) {
        String left = safe(key);
        String right = safe(value);
        String raw = left + ": " + right;
        if (raw.length() <= PAPER_WIDTH) {
            int spaces = PAPER_WIDTH - raw.length();
            return left + ": " + " ".repeat(Math.max(spaces, 1)) + right;
        }
        return left + ": " + right;
    }

    private void appendWrappedLabel(StringBuilder sb, String label, String value, int indent) {
        String full = label + ": " + safe(value);
        appendWrapped(sb, full, indent);
    }

    private void appendWrapped(StringBuilder sb, String text, int indent) {
        String normalized = safe(text);
        int contentWidth = Math.max(PAPER_WIDTH - Math.max(indent, 0), 10);
        String prefix = " ".repeat(Math.max(indent, 0));
        for (String line : wrap(normalized, contentWidth)) {
            sb.append(prefix).append(line).append("\n");
        }
    }

    private List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }
        String remaining = text.trim();
        while (remaining.length() > width) {
            int breakAt = remaining.lastIndexOf(' ', width);
            if (breakAt <= 0) {
                breakAt = width;
            }
            lines.add(remaining.substring(0, breakAt).trim());
            remaining = remaining.substring(breakAt).trim();
        }
        if (!remaining.isEmpty()) {
            lines.add(remaining);
        }
        return lines;
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value
                .replace("\r", " ")
                .replace("\n", " ")
                .replace("\t", " ")
                .trim();
    }

    private String valueOrDash(String value) {
        return safe(value);
    }

    private void appendPagoFactura(StringBuilder sb, Venta venta, BigDecimal pagoEfectivo, BigDecimal pagoTransferencia) {
        BigDecimal efectivo = safeNonNegative(pagoEfectivo);
        BigDecimal transferencia = safeNonNegative(pagoTransferencia);
        boolean mixto = efectivo.compareTo(BigDecimal.ZERO) > 0 && transferencia.compareTo(BigDecimal.ZERO) > 0;

        if (mixto) {
            sb.append(kvLine("PAGO", "MIXTO")).append("\n");
            sb.append(kvLine("  Efectivo", "$" + formatMoneda(efectivo))).append("\n");
            sb.append(kvLine("  Transfer", "$" + formatMoneda(transferencia))).append("\n");
            return;
        }

        if (efectivo.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(kvLine("PAGO", "EFECTIVO")).append("\n");
            sb.append(kvLine("  Valor", "$" + formatMoneda(efectivo))).append("\n");
            return;
        }
        if (transferencia.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(kvLine("PAGO", "TRANSFERENCIA")).append("\n");
            sb.append(kvLine("  Valor", "$" + formatMoneda(transferencia))).append("\n");
            return;
        }

        sb.append(kvLine("PAGO", valueOrDash(venta.getFormaPago() == null ? null : venta.getFormaPago().name()))).append("\n");
    }

    private BigDecimal safeNonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void appendTrailingFeed(StringBuilder sb) {
        sb.append("\n".repeat(TRAILING_FEED_LINES));
    }

    private void enviarAImpresora(String texto) {
        try {
            PrintService printer = PrintServiceLookup.lookupDefaultPrintService();
            if (printer == null) {
                throw new IllegalStateException("No hay impresora predeterminada configurada");
            }

            DocPrintJob job = printer.createPrintJob();
            byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
            Doc doc = new SimpleDoc(bytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
            job.print(doc, null);
        } catch (Exception e) {
            throw new RuntimeException("Error imprimiendo ticket", e);
        }
    }
}
