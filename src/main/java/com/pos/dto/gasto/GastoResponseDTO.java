package com.pos.dto.gasto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class GastoResponseDTO {

    private Long id;
    private LocalDateTime fecha;
    private String descripcion;
    private BigDecimal monto;
    private String origen; // CAJA | ADMIN

    public GastoResponseDTO(
            Long id,
            LocalDateTime fecha,
            String descripcion,
            BigDecimal monto,
            String origen
    ) {
        this.id = id;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.monto = monto;
        this.origen = origen;
    }

    // getters y setters
}

