package com.pos.dto.report;

import com.pos.dto.venta.VentaResponseDTO;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ReporteVentaDTO {

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    private Long totalVentas;

    private BigDecimal totalBruto;
    private BigDecimal totalDescuentos;
    private BigDecimal totalNeto;

    private BigDecimal totalEfectivo;
    private BigDecimal totalTransferencia;

    private List<VentaResponseDTO> ventas;

    public ReporteVentaDTO() {
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Long getTotalVentas() {
        return totalVentas;
    }

    public void setTotalVentas(Long totalVentas) {
        this.totalVentas = totalVentas;
    }

    public BigDecimal getTotalBruto() {
        return totalBruto;
    }

    public void setTotalBruto(BigDecimal totalBruto) {
        this.totalBruto = totalBruto;
    }

    public BigDecimal getTotalDescuentos() {
        return totalDescuentos;
    }

    public void setTotalDescuentos(BigDecimal totalDescuentos) {
        this.totalDescuentos = totalDescuentos;
    }

    public BigDecimal getTotalNeto() {
        return totalNeto;
    }

    public void setTotalNeto(BigDecimal totalNeto) {
        this.totalNeto = totalNeto;
    }

    public BigDecimal getTotalEfectivo() {
        return totalEfectivo;
    }

    public void setTotalEfectivo(BigDecimal totalEfectivo) {
        this.totalEfectivo = totalEfectivo;
    }

    public BigDecimal getTotalTransferencia() {
        return totalTransferencia;
    }

    public void setTotalTransferencia(BigDecimal totalTransferencia) {
        this.totalTransferencia = totalTransferencia;
    }

    public List<VentaResponseDTO> getVentas() {
        return ventas;
    }

    public void setVentas(List<VentaResponseDTO> ventas) {
        this.ventas = ventas;
    }
}
