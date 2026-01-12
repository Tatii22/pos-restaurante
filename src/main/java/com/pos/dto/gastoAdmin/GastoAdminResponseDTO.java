package com.pos.dto.gastoAdmin;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class GastoAdminResponseDTO {

    private Long id;
    private LocalDate fecha;
    private String descripcion;
    private BigDecimal monto;
    private String tipoGasto;
    private String usuario;
}
