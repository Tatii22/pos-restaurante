package com.pos.service;

import com.pos.dto.turno.GastoCajaCreateDTO;
import com.pos.dto.turno.GastoCajaResponseDTO;
import com.pos.entity.*;
import com.pos.exception.BadRequestException;
import com.pos.repository.GastoCajaRepository;
import com.pos.repository.TipoGastoRepository;
import com.pos.repository.TurnoCajaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
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

        // 🔐 Solo CAJA
        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo CAJA puede registrar gastos de caja");
        }

        // 🔓 Turno abierto
        TurnoCaja turno = turnoCajaRepository
                .findByEstado(EstadoTurno.ABIERTO)
                .orElseThrow(() -> new BadRequestException("No hay turno abierto"));

        // 🏷️ Tipo de gasto
        TipoGasto tipo = tipoGastoRepository.findById(dto.tipoGastoId())
                .orElseThrow(() -> new BadRequestException("Tipo de gasto no existe"));


        GastoCaja gasto = GastoCaja.builder()
                .descripcion(dto.descripcion())
                .monto(dto.monto())
                .fecha(LocalDateTime.now())
                .tipo(tipo)
                .turno(turno)
                .usuario(usuario)
                .build();

        // 🔥 Impacto directo en el turno
        turno.setTotalGastos(
                turno.getTotalGastos().add(dto.monto())
        );

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
                .map(g -> new GastoCajaResponseDTO(
                        g.getId(),
                        g.getFecha(),
                        g.getDescripcion(),
                        g.getMonto()
                ))
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
                .map(g -> new GastoCajaResponseDTO(
                        g.getId(),
                        g.getFecha(),
                        g.getDescripcion(),
                        g.getMonto()
                ))
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
}
