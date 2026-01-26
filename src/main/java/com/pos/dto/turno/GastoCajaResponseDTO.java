package com.pos.dto.turno;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class GastoCajaResponseDTO {

    private Long id;
    private LocalDateTime fecha;
    private String descripcion;
    private BigDecimal valor;

    public GastoCajaResponseDTO() {}

    public GastoCajaResponseDTO(Long id, LocalDateTime fecha, String descripcion, BigDecimal valor) {
        this.id = id;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.valor = valor;
    }

}
