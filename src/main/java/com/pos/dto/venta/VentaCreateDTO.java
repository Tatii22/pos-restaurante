package com.pos.dto.venta;

import com.pos.entity.FormaPago;
import com.pos.entity.TipoVenta;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
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
        @DecimalMin(value = "0.0", inclusive = true)
        @DecimalMax(value = "100.0", inclusive = true)
        BigDecimal descuentoPorcentaje,

        // Opcional
        BigDecimal valorDomicilio,

        @NotEmpty(message = "La venta debe tener al menos un producto")
        List<VentaDetalleCreateDTO> detalles
) {}
