package com.pos.service;

import com.pos.dto.turno.GastoCajaCreateDTO;
import com.pos.dto.turno.GastoCajaResponseDTO;
import com.pos.entity.EstadoTurno;
import com.pos.entity.GastoCaja;
import com.pos.entity.TipoGasto;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Usuario;
import com.pos.exception.BadRequestException;
import com.pos.repository.GastoCajaRepository;
import com.pos.repository.TipoGastoRepository;
import com.pos.repository.TurnoCajaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GastoCajaService {

    private final GastoCajaRepository gastoCajaRepository;
    private final TurnoCajaRepository turnoCajaRepository;
    private final TipoGastoRepository tipoGastoRepository;

    @Transactional
    public GastoCaja registrar(
            GastoCajaCreateDTO dto,
            Usuario usuario
    ) {
        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo CAJA puede registrar gastos de caja");
        }

        TurnoCaja turno = turnoCajaRepository
                .findByEstadoIn(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .orElseThrow(() -> new BadRequestException("No hay turno activo"));

        TipoGasto tipo = tipoGastoRepository.findById(dto.tipoGastoId())
                .orElseThrow(() -> new BadRequestException("Tipo de gasto no existe"));

        BigDecimal montoEfectivo = resolverMontoEfectivo(dto.monto(), dto.montoEfectivo(), dto.montoTransferencia());
        BigDecimal montoTransferencia = nonNegative(dto.montoTransferencia());
        BigDecimal montoTotal = montoEfectivo.add(montoTransferencia);
        validarMontoTotal(montoTotal);

        GastoCaja gasto = GastoCaja.builder()
                .descripcion(dto.descripcion())
                .monto(montoTotal)
                .montoEfectivo(montoEfectivo)
                .montoTransferencia(montoTransferencia)
                .fecha(LocalDateTime.now())
                .tipo(tipo)
                .turno(turno)
                .usuario(usuario)
                .build();

        turno.setTotalGastos(turno.getTotalGastos().add(montoTotal));
        turnoCajaRepository.save(turno);

        return gastoCajaRepository.save(gasto);
    }

    public List<GastoCajaResponseDTO> listarTurnoActivo(Usuario usuario) {
        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo CAJA puede ver gastos de caja");
        }

        TurnoCaja turno = turnoCajaRepository
                .findByEstadoIn(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .orElseThrow(() -> new BadRequestException("No hay turno activo"));

        return gastoCajaRepository.findByTurnoOrderByFechaDesc(turno)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<GastoCajaResponseDTO> listarPorRango(LocalDate inicio, LocalDate fin, Usuario usuario) {
        if (usuario == null || usuario.getRol() == null) {
            throw new BadRequestException("Usuario no valido");
        }
        String rol = usuario.getRol().getNombre();
        if (!"CAJA".equals(rol) && !"ADMIN".equals(rol)) {
            throw new BadRequestException("No autorizado para ver gastos de caja");
        }
        if (inicio == null || fin == null) {
            throw new BadRequestException("Las fechas son obligatorias");
        }
        if (inicio.isAfter(fin)) {
            throw new BadRequestException("La fecha inicio no puede ser mayor a la fecha fin");
        }

        LocalDateTime desde = inicio.atStartOfDay();
        LocalDateTime hasta = fin.atTime(23, 59, 59);

        return gastoCajaRepository.findByFechaBetween(desde, hasta)
                .stream()
                .sorted((a, b) -> b.getFecha().compareTo(a.getFecha()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void eliminarPorId(Long id, Usuario usuario) {
        if (usuario == null || usuario.getRol() == null || !"ADMIN".equals(usuario.getRol().getNombre())) {
            throw new BadRequestException("Solo ADMIN puede eliminar gastos de caja");
        }

        GastoCaja gasto = gastoCajaRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Gasto de caja no encontrado"));

        TurnoCaja turno = gasto.getTurno();
        if (turno != null && turno.getTotalGastos() != null) {
            BigDecimal nuevoTotal = turno.getTotalGastos().subtract(gasto.getMonto());
            turno.setTotalGastos(nuevoTotal.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : nuevoTotal);
            turnoCajaRepository.save(turno);
        }

        gastoCajaRepository.delete(gasto);
    }

    private GastoCajaResponseDTO toResponse(GastoCaja gasto) {
        return new GastoCajaResponseDTO(
                gasto.getId(),
                gasto.getFecha(),
                gasto.getDescripcion(),
                gasto.getMonto(),
                nonNegative(gasto.getMontoEfectivo()),
                nonNegative(gasto.getMontoTransferencia())
        );
    }

    private BigDecimal resolverMontoEfectivo(BigDecimal montoLegacy, BigDecimal montoEfectivo, BigDecimal montoTransferencia) {
        BigDecimal efectivo = nonNegative(montoEfectivo);
        BigDecimal transferencia = nonNegative(montoTransferencia);
        if (efectivo.compareTo(BigDecimal.ZERO) == 0
                && transferencia.compareTo(BigDecimal.ZERO) == 0
                && montoLegacy != null) {
            return nonNegative(montoLegacy);
        }
        return efectivo;
    }

    private void validarMontoTotal(BigDecimal montoTotal) {
        if (montoTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Debes registrar un monto mayor a 0 en efectivo, transferencia o ambos");
        }
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }
}
