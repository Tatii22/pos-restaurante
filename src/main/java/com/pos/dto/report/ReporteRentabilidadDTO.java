package com.pos.dto.report;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.pos.dto.gasto.GastoResponseDTO;
import com.pos.dto.venta.VentaResponseDTO;

@Getter
@Setter
public class ReporteRentabilidadDTO {

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    private BigDecimal totalVentas = BigDecimal.ZERO;
    private BigDecimal totalGastos = BigDecimal.ZERO;
    private BigDecimal gananciaNeta = BigDecimal.ZERO;

    private List<VentaResponseDTO> ventas;
    private List<GastoResponseDTO> gastos; 

    
}

