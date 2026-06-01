package com.pos.service;

import com.pos.dto.venta.VentaPagoDetalleDTO;
import com.pos.entity.Venta;
import com.pos.entity.VentaPagoDetalle;
import com.pos.repository.VentaPagoDetalleRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class VentaPagoDetalleService {

    private final VentaPagoDetalleRepository ventaPagoDetalleRepository;
    private final EntityManager entityManager;

    @Transactional
    public void guardar(Long ventaId, BigDecimal pagoEfectivo, BigDecimal pagoTransferencia) {
        guardar(ventaId, pagoEfectivo, pagoTransferencia, pagoEfectivo, pagoTransferencia, BigDecimal.ZERO);
    }

    @Transactional
    public void guardar(
            Long ventaId,
            BigDecimal pagoEfectivo,
            BigDecimal pagoTransferencia,
            BigDecimal recibidoEfectivo,
            BigDecimal recibidoTransferencia,
            BigDecimal cambioEfectivo
    ) {
        if (ventaId == null) {
            return;
        }
        BigDecimal efectivo = nonNegative(pagoEfectivo);
        BigDecimal transferencia = nonNegative(pagoTransferencia);

        VentaPagoDetalle detalle = ventaPagoDetalleRepository.findById(ventaId)
                .orElseGet(() -> VentaPagoDetalle.builder()
                        .venta(entityManager.getReference(Venta.class, ventaId))
                        .build());

        detalle.setPagoEfectivo(efectivo);
        detalle.setPagoTransferencia(transferencia);
        detalle.setRecibidoEfectivo(nonNegative(recibidoEfectivo));
        detalle.setRecibidoTransferencia(nonNegative(recibidoTransferencia));
        detalle.setCambioEfectivo(nonNegative(cambioEfectivo));

        ventaPagoDetalleRepository.save(detalle);
    }

    @Transactional(readOnly = true)
    public VentaPagoDetalleDTO obtener(Long ventaId) {
        if (ventaId == null) {
            return null;
        }
        return ventaPagoDetalleRepository.findById(ventaId)
                .map(detalle -> new VentaPagoDetalleDTO(
                        detalle.getPagoEfectivo(),
                        detalle.getPagoTransferencia(),
                        detalle.getRecibidoEfectivo(),
                        detalle.getRecibidoTransferencia(),
                        detalle.getCambioEfectivo()
                ))
                .orElse(null);
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }
}
