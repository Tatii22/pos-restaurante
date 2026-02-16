package com.pos.service;

import com.pos.dto.venta.VentaResponseDTO;
import com.pos.entity.Venta;
import com.pos.entity.VentaDetalle;
import org.springframework.stereotype.Service;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;

@Service
public class ImpresoraTtermicaService {

    public void imprimirFactura(Venta venta) {
        StringBuilder sb = new StringBuilder();

        sb.append("********  FACTURA  ********\n");
        sb.append("No: ").append(venta.getId()).append("\n");
        sb.append("Fecha: ").append(venta.getFecha()).append("\n");
        sb.append("Tipo: ").append(venta.getTipoVenta()).append("\n");
        sb.append("Estado: ").append(venta.getEstado()).append("\n");
        sb.append("Cliente: ")
                .append(venta.getClienteNombre() != null ? venta.getClienteNombre() : "-")
                .append("\n");
        sb.append("---------------------------\n");

        BigDecimal subtotal = BigDecimal.ZERO;
        if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
            for (VentaDetalle detalle : venta.getDetalles()) {
                String nombreProducto = detalle.getProducto() != null
                        ? detalle.getProducto().getNombre()
                        : "Producto";

                sb.append(detalle.getCantidad())
                        .append(" x ")
                        .append(nombreProducto)
                        .append(" @ $")
                        .append(formatMoneda(detalle.getPrecioUnitario()))
                        .append(" = $")
                        .append(formatMoneda(detalle.getSubtotal()))
                        .append("\n");

                if (detalle.getObservacion() != null && !detalle.getObservacion().isBlank()) {
                    sb.append("  obs: ").append(detalle.getObservacion()).append("\n");
                }
                subtotal = subtotal.add(detalle.getSubtotal() != null ? detalle.getSubtotal() : BigDecimal.ZERO);
            }
        }

        sb.append("---------------------------\n");
        sb.append("SUBTOTAL: $").append(formatMoneda(subtotal)).append("\n");
        if (venta.getValorDomicilio() != null) {
            sb.append("DOMICILIO: $").append(formatMoneda(venta.getValorDomicilio())).append("\n");
        }
        if (venta.getDescuentoValor() != null && venta.getDescuentoValor().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("DESCUENTO: -$").append(formatMoneda(venta.getDescuentoValor())).append("\n");
        }
        sb.append("TOTAL: $").append(formatMoneda(venta.getTotal())).append("\n");
        sb.append("Pago: ").append(venta.getFormaPago()).append("\n");
        sb.append("***************************\n\n");

        enviarAImpresora(sb.toString());
    }

    public void imprimirTicketVenta(Venta venta) {
        imprimirFactura(venta);
    }

    public void imprimirTicketCocina(Venta venta) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append("================================\n");
        sb.append("          TICKET COCINA         \n");
        sb.append("================================\n");
        sb.append("PEDIDO : ").append(venta.getId()).append("\n");
        sb.append("FECHA  : ").append(venta.getFecha()).append("\n");
        sb.append("CLIENTE: ")
                .append(venta.getClienteNombre() != null && !venta.getClienteNombre().isBlank()
                        ? venta.getClienteNombre()
                        : "SIN NOMBRE")
                .append("\n");
        sb.append("--------------------------------\n");

        if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
            int linea = 1;
            for (VentaDetalle detalle : venta.getDetalles()) {
                String nombreProducto = detalle.getProducto() != null
                        ? detalle.getProducto().getNombre()
                        : "Producto";

                sb.append(linea++)
                        .append(". ")
                        .append(detalle.getCantidad())
                        .append(" x ")
                        .append(nombreProducto)
                        .append("\n");

                if (detalle.getObservacion() != null && !detalle.getObservacion().isBlank()) {
                    sb.append("   -> DETALLE: ").append(detalle.getObservacion()).append("\n");
                }
                sb.append("\n");
            }
        } else {
            sb.append("SIN DETALLE DE PRODUCTOS\n");
        }

        sb.append("--------------------------------\n");
        sb.append("   ENVIAR A PREPARACION\n");
        sb.append("================================\n\n");
        enviarAImpresora(sb.toString());
    }

    // Fallback para flujos que solo tienen DTO resumido.
    public void imprimirTicketVenta(VentaResponseDTO venta) {
        StringBuilder sb = new StringBuilder();

        sb.append("**** TICKET DE VENTA ****\n");
        sb.append("ID: ").append(venta.id()).append("\n");
        sb.append("Fecha: ").append(venta.fecha()).append("\n");
        sb.append("Tipo: ").append(venta.tipoVenta()).append("\n");
        sb.append("Estado: ").append(venta.estado()).append("\n");
        sb.append("Cliente: ")
                .append(venta.clienteNombre() != null ? venta.clienteNombre() : "-")
                .append("\n");
        sb.append("-------------------------\n");
        sb.append("TOTAL: $").append(formatMoneda(venta.total())).append("\n");
        sb.append("Forma Pago: ").append(venta.formaPago()).append("\n");
        sb.append("*************************\n\n");

        enviarAImpresora(sb.toString());
    }

    private String formatMoneda(BigDecimal valor) {
        if (valor == null) {
            return "0.00";
        }
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
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
