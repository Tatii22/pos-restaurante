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
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class ImpresoraTtermicaService {

    private static final int PAPER_WIDTH = 48;

    // FIX 3: Incluir hora en la fecha del ticket
    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final int TRAILING_FEED_LINES = 8;
    private static final String LINE        = "-".repeat(PAPER_WIDTH);
    private static final String DOUBLE_LINE = "=".repeat(PAPER_WIDTH);

    // FIX 5: Formato colombiano — puntos como separador de miles, sin decimales
    private static final NumberFormat COP_FORMAT;
    static {
        COP_FORMAT = NumberFormat.getIntegerInstance(new Locale("es", "CO"));
        COP_FORMAT.setGroupingUsed(true);
    }

    private final ConfiguracionService     configuracionService;
    private final VentaPagoDetalleService  ventaPagoDetalleService;

    // ─────────────────────────────────────────────────────────────────────────
    // API PÚBLICA
    // ─────────────────────────────────────────────────────────────────────────

    public void imprimirFactura(Venta venta) {
        imprimirFactura(venta, null, null);
    }

    public void imprimirFactura(Venta venta, BigDecimal pagoEfectivo, BigDecimal pagoTransferencia) {
        AdminConfigDTO cfg = configuracionService.obtener();

        BigDecimal efectivoFinal      = pagoEfectivo;
        BigDecimal transferenciaFinal = pagoTransferencia;

        if (efectivoFinal == null && transferenciaFinal == null
                && venta != null && venta.getId() != null) {
            VentaPagoDetalleDTO detallePago = ventaPagoDetalleService.obtener(venta.getId());
            if (detallePago != null) {
                efectivoFinal      = detallePago.pagoEfectivo();
                transferenciaFinal = detallePago.pagoTransferencia();
            }
        }

        StringBuilder sb = new StringBuilder();

        // Encabezado del negocio
        appendCentered(sb, safe(cfg.negocioNombre()));
        if (!isBlank(cfg.negocioNit()))       appendKvLabel(sb, "NIT",  cfg.negocioNit());
        if (!isBlank(cfg.negocioTelefono()))  appendKvLabel(sb, "Tel",  cfg.negocioTelefono());
        if (!isBlank(cfg.negocioDireccion())) appendKvLabel(sb, "Dir",  cfg.negocioDireccion());
        if (!isBlank(cfg.ticketEncabezado())) appendCentered(sb, cfg.ticketEncabezado());

        sb.append(DOUBLE_LINE).append("\n");
        appendCentered(sb, "FACTURA");
        sb.append(DOUBLE_LINE).append("\n");

        // Datos de la venta
        appendKv(sb, "No",      String.valueOf(venta.getId()));
        appendKv(sb, "Fecha",   formatFecha(venta.getFecha()));
        appendKv(sb, "Tipo",    valueOrDash(venta.getTipoVenta()  == null ? null : venta.getTipoVenta().name()));
        appendKv(sb, "Estado",  valueOrDash(venta.getEstado()     == null ? null : venta.getEstado().name()));
        appendKvLabel(sb, "Cliente", valueOrDash(venta.getClienteNombre()));

        if (venta.getTipoVenta() == TipoVenta.DOMICILIO) {
            appendKvLabel(sb, "Telefono",  valueOrDash(venta.getTelefono()));
            appendKvLabel(sb, "Direccion", valueOrDash(venta.getDireccion()));
        }

        sb.append(LINE).append("\n");

        // Detalle de productos
        BigDecimal subtotal = BigDecimal.ZERO;
        if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
            for (VentaDetalle detalle : venta.getDetalles()) {
                String nombreProducto = detalle.getProducto() != null
                        ? detalle.getProducto().getNombre() : "Producto";

                String itemLabel = detalle.getCantidad() + " x " + safe(nombreProducto);
                String itemTotal = "$" + formatMoneda(detalle.getSubtotal());

                // FIX 4: Columnas alineadas correctamente
                appendKvAligned(sb, itemLabel, itemTotal);

                if (detalle.getObservacion() != null && !detalle.getObservacion().isBlank()) {
                    appendKvLabel(sb, "  Obs", detalle.getObservacion());
                }
                sb.append(LINE).append("\n");
                subtotal = subtotal.add(
                        detalle.getSubtotal() != null ? detalle.getSubtotal() : BigDecimal.ZERO);
            }
        }

        // Totales
        appendKvAligned(sb, "SUBTOTAL",  "$" + formatMoneda(subtotal));
        if (venta.getValorDomicilio() != null && venta.getValorDomicilio().compareTo(BigDecimal.ZERO) > 0) {
            appendKvAligned(sb, "DOMICILIO", "$" + formatMoneda(venta.getValorDomicilio()));
        }
        if (venta.getDescuentoValor() != null
                && venta.getDescuentoValor().compareTo(BigDecimal.ZERO) > 0) {
            appendKvAligned(sb, "DESCUENTO", "-$" + formatMoneda(venta.getDescuentoValor()));
        }

        sb.append(DOUBLE_LINE).append("\n");
        appendKvAligned(sb, "TOTAL", "$" + formatMoneda(venta.getTotal()));
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
        if (venta.getTipoVenta() == TipoVenta.DOMICILIO) appendCentered(sb, "PEDIDO DOMICILIO");
        if (Boolean.TRUE.equals(venta.getParaLlevar()))  appendCentered(sb, "PARA LLEVAR");

        sb.append(DOUBLE_LINE).append("\n");
        appendKv(sb, "Pedido", String.valueOf(venta.getId()));
        appendKv(sb, "Fecha",  formatFecha(venta.getFecha()));
        appendKvLabel(sb, "Cliente", valueOrDash(venta.getClienteNombre()));
        sb.append(LINE).append("\n");

        if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
            for (VentaDetalle detalle : venta.getDetalles()) {
                String nombreProducto = detalle.getProducto() != null
                        ? detalle.getProducto().getNombre() : "Producto";
                appendWrapped(sb, "* " + detalle.getCantidad() + " x " + safe(nombreProducto), 0);
                if (detalle.getObservacion() != null && !detalle.getObservacion().isBlank()) {
                    appendKvLabel(sb, "   Obs", detalle.getObservacion());
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

    /** Fallback para flujos que solo tienen DTO resumido. */
    public void imprimirTicketVenta(VentaResponseDTO venta) {
        StringBuilder sb = new StringBuilder();
        appendCentered(sb, "TICKET DE VENTA");
        sb.append(DOUBLE_LINE).append("\n");
        appendKv(sb, "ID",     String.valueOf(venta.id()));
        appendKv(sb, "Fecha",  valueOrDash(String.valueOf(venta.fecha())));
        appendKv(sb, "Tipo",   valueOrDash(venta.tipoVenta()  == null ? null : venta.tipoVenta().name()));
        appendKv(sb, "Estado", valueOrDash(venta.estado()     == null ? null : venta.estado().name()));
        appendKvLabel(sb, "Cliente", valueOrDash(venta.clienteNombre()));
        sb.append(DOUBLE_LINE).append("\n");
        appendKvAligned(sb, "TOTAL", "$" + formatMoneda(venta.total()));
        appendKv(sb, "PAGO",   valueOrDash(venta.formaPago() == null ? null : venta.formaPago().name()));
        sb.append(DOUBLE_LINE).append("\n");
        appendTrailingFeed(sb);
        enviarAImpresora(sb.toString());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FORMATEO MONETARIO (FIX 5)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Formatea un BigDecimal como moneda colombiana: sin decimales, punto como separador de miles.
     * Ejemplos: 8500 → "8.500"  |  1200000 → "1.200.000"  |  null → "0"
     */
    private String formatMoneda(BigDecimal valor) {
        if (valor == null) return "0";
        // Redondear a entero (COP no usa centavos en operación diaria)
        long entero = valor.setScale(0, RoundingMode.HALF_UP).longValueExact();
        return COP_FORMAT.format(entero);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ALINEACIÓN DE COLUMNAS (FIX 4)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Imprime una fila con clave alineada a la izquierda y valor alineado a la derecha
     * en posición fija (columna PAPER_WIDTH).
     *
     *   "TOTAL:                          $8.500"
     *   "SUBTOTAL:                       $8.000"
     *   "DOMICILIO:                      $2.500"
     */
    private void appendKvAligned(StringBuilder sb, String key, String value) {
        String left  = safe(key);
        String right = safe(value);
        // "KEY:  " = left + ": "
        String prefix = left + ": ";
        int rightWidth = right.length();
        int spaces = PAPER_WIDTH - prefix.length() - rightWidth;

        if (spaces >= 0) {
            sb.append(prefix)
              .append(" ".repeat(spaces))
              .append(right)
              .append("\n");
        } else {
            // No cabe en una línea → key en su propia línea, valor alineado a la derecha en siguiente
            sb.append(prefix.trim()).append("\n");
            int pad = Math.max(0, PAPER_WIDTH - rightWidth);
            sb.append(" ".repeat(pad)).append(right).append("\n");
        }
    }

    /** Fila clave-valor simple para datos informativos (no financieros). */
    private void appendKv(StringBuilder sb, String key, String value) {
        sb.append(safe(key)).append(": ").append(safe(value)).append("\n");
    }

    /** Fila con etiqueta que puede hacer wrap si el valor es largo. */
    private void appendKvLabel(StringBuilder sb, String label, String value) {
        appendWrapped(sb, safe(label) + ": " + safe(value), 0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAGO EN FACTURA
    // ─────────────────────────────────────────────────────────────────────────

    private void appendPagoFactura(StringBuilder sb, Venta venta,
                                    BigDecimal pagoEfectivo, BigDecimal pagoTransferencia) {
        if (venta.getFormaPago() == com.pos.entity.FormaPago.FIADO) {
            appendKvAligned(sb, "PAGO",    "FIADO (pendiente)");
            appendKvAligned(sb, "  Saldo", "$" + formatMoneda(venta.getSaldoPendiente()));
            return;
        }

        BigDecimal efectivo      = safeNonNegative(pagoEfectivo);
        BigDecimal transferencia = safeNonNegative(pagoTransferencia);
        boolean tieneEfectivo      = efectivo.compareTo(BigDecimal.ZERO)      > 0;
        boolean tieneTransferencia = transferencia.compareTo(BigDecimal.ZERO) > 0;

        if (tieneEfectivo && tieneTransferencia) {
            appendKvAligned(sb, "PAGO",        "MIXTO");
            appendKvAligned(sb, "  Efectivo",  "$" + formatMoneda(efectivo));
            appendKvAligned(sb, "  Transfer",  "$" + formatMoneda(transferencia));
            return;
        }
        if (tieneEfectivo) {
            appendKvAligned(sb, "PAGO",    "EFECTIVO");
            appendKvAligned(sb, "  Valor", "$" + formatMoneda(efectivo));
            return;
        }
        if (tieneTransferencia) {
            appendKvAligned(sb, "PAGO",    "TRANSFERENCIA");
            appendKvAligned(sb, "  Valor", "$" + formatMoneda(transferencia));
            return;
        }

        appendKvAligned(sb, "PAGO",
                valueOrDash(venta.getFormaPago() == null ? null : venta.getFormaPago().name()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS DE TEXTO
    // ─────────────────────────────────────────────────────────────────────────

    private String formatFecha(LocalDateTime fecha) {
        return fecha == null ? "-" : DATE_TIME_FMT.format(fecha);
    }

    private void appendCentered(StringBuilder sb, String text) {
        String t = safe(text);
        if (t.length() >= PAPER_WIDTH) { appendWrapped(sb, t, 0); return; }
        int leftPad = (PAPER_WIDTH - t.length()) / 2;
        sb.append(" ".repeat(Math.max(leftPad, 0))).append(t).append("\n");
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
        if (text == null || text.isBlank()) { lines.add(""); return lines; }
        String remaining = text.trim();
        while (remaining.length() > width) {
            int breakAt = remaining.lastIndexOf(' ', width);
            if (breakAt <= 0) breakAt = width;
            lines.add(remaining.substring(0, breakAt).trim());
            remaining = remaining.substring(breakAt).trim();
        }
        if (!remaining.isEmpty()) lines.add(remaining);
        return lines;
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) return "-";
        return value.replace("\r", " ").replace("\n", " ").replace("\t", " ").trim();
    }

    private String valueOrDash(String value) { return safe(value); }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    private BigDecimal safeNonNegative(BigDecimal value) {
        return (value == null || value.compareTo(BigDecimal.ZERO) < 0) ? BigDecimal.ZERO : value;
    }

    private void appendTrailingFeed(StringBuilder sb) {
        sb.append("\n".repeat(TRAILING_FEED_LINES));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENVÍO A IMPRESORA
    // ─────────────────────────────────────────────────────────────────────────

    private void enviarAImpresora(String texto) {
        try {
            PrintService printer = PrintServiceLookup.lookupDefaultPrintService();
            if (printer == null) {
                throw new IllegalStateException("No hay impresora predeterminada configurada");
            }
            DocPrintJob job  = printer.createPrintJob();
            byte[]      bytes = texto.getBytes(StandardCharsets.UTF_8);
            Doc         doc   = new SimpleDoc(bytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
            job.print(doc, null);
        } catch (Exception e) {
            throw new RuntimeException("Error imprimiendo ticket: " + e.getMessage(), e);
        }
    }
}
