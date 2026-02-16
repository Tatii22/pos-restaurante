package com.pos.service;

import com.pos.entity.EstadoTurno;
import com.pos.entity.EstadoVenta;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Usuario;
import com.pos.exception.BadRequestException;
import com.pos.repository.InventarioDiarioRepository;
import com.pos.repository.TurnoCajaRepository;
import com.pos.repository.UsuarioRepository;
import com.pos.repository.VentaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TurnoCajaService {

    private final TurnoCajaRepository turnoCajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final VentaRepository ventaRepository;
    private final InventarioDiarioRepository inventarioDiarioRepository;

    @Transactional
    public TurnoCaja abrirTurno(BigDecimal montoInicial, String username) {

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Usuario no existe"));

        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo un usuario CAJA puede abrir turno");
        }

        if (turnoCajaRepository.existsByEstadoIn(
                List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))) {
            throw new BadRequestException("Ya existe un turno activo");
        }

        // Reinicia TODO el inventario del día al iniciar cada turno.
        inventarioDiarioRepository.deleteByFecha(LocalDate.now());

        TurnoCaja turno = TurnoCaja.builder()
                .fechaApertura(LocalDateTime.now())
                .montoInicial(montoInicial)
                .totalVentas(BigDecimal.ZERO)
                .totalGastos(BigDecimal.ZERO)
                .estado(EstadoTurno.ABIERTO)
                .usuario(usuario)
                .build();

        return turnoCajaRepository.save(turno);
    }

    public TurnoCaja simularCierre(BigDecimal efectivoContado, Usuario usuario) {

        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo CAJA puede simular cierre");
        }

        TurnoCaja turno = turnoCajaRepository
                .findByEstadoIn(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .orElseThrow(() -> new BadRequestException("No hay turno abierto"));

        BigDecimal totalVentas = ventaRepository
                .sumarTotalPorTurnoPorEstado(turno.getId(), EstadoVenta.DESPACHADA);

        turno.setTotalVentas(totalVentas);

        BigDecimal esperado = turno.getMontoInicial()
                .add(totalVentas)
                .subtract(turno.getTotalGastos());

        BigDecimal faltante = esperado.subtract(efectivoContado);

        turno.setEsperado(esperado);
        turno.setFaltante(faltante);
        turno.setEstado(EstadoTurno.SIMULADO);

        return turnoCajaRepository.save(turno);
    }

    public TurnoCaja cerrarTurno(BigDecimal montoFinal, Usuario usuario) {

        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo CAJA puede cerrar turno");
        }

        TurnoCaja turno = turnoCajaRepository
                .findByEstadoIn(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .orElseThrow(() -> new BadRequestException("No hay turno para cerrar"));

        BigDecimal totalVentas = ventaRepository
                .sumarTotalPorTurnoPorEstado(turno.getId(), EstadoVenta.DESPACHADA);

        turno.setTotalVentas(totalVentas);

        BigDecimal esperado = turno.getMontoInicial()
                .add(totalVentas)
                .subtract(turno.getTotalGastos());

        BigDecimal faltante = esperado.subtract(montoFinal);

        turno.setEsperado(esperado);
        turno.setMontoFinal(montoFinal);
        turno.setFaltante(faltante);
        turno.setFechaCierre(LocalDateTime.now());
        turno.setEstado(EstadoTurno.CERRADO);

        return turnoCajaRepository.save(turno);
    }

    public TurnoCaja obtenerTurnoActivo() {
        return turnoCajaRepository
                .findByEstadoIn(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .orElse(null);
    }
}
