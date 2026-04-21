package com.pos.dto.venta;

import com.pos.entity.FormaPago;
import com.pos.entity.TipoVenta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

public record VentaCreateDTO(

        @NotNull(message = "El tipo de venta es obligatorio")
        TipoVenta tipoVenta,

        @NotNull(message = "La forma de pago es obligatoria")
        FormaPago formaPago,

        // Datos cliente (solo obligatorios en DOMICILIO)
        String clienteNombre,
        String telefono,
        String direccion,

        // Opcional
        @DecimalMin(value = "0.0", inclusive = true, message = "El descuento no puede ser negativo")
        @DecimalMax(value = "100.0", inclusive = true, message = "El descuento no puede ser mayor a 100")
        BigDecimal descuentoPorcentaje,

        // Opcional
        @PositiveOrZero(message = "El valor domicilio no puede ser negativo")
        BigDecimal valorDomicilio,

        Boolean paraLlevar,

        // Opcional: permite reflejar pago mixto en ticket/factura
        @PositiveOrZero(message = "El pago en efectivo no puede ser negativo")
        BigDecimal pagoEfectivo,
        @PositiveOrZero(message = "El pago por transferencia no puede ser negativo")
        BigDecimal pagoTransferencia,

        @NotEmpty(message = "La venta debe tener al menos un producto")
        @Valid
        List<VentaDetalleCreateDTO> detalles
) {}
