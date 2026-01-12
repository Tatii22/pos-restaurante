package com.pos.service;

import com.pos.entity.TurnoCaja;
import com.pos.entity.Usuario;
import com.pos.entity.EstadoTurno;
import com.pos.exception.BadRequestException;
import com.pos.repository.TurnoCajaRepository;
import com.pos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.pos.repository.VentaRepository;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TurnoCajaService {

    private final TurnoCajaRepository turnoCajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final VentaRepository ventaRepository; // 🔥 NUEVO

    // 🔓 APERTURA DE TURNO
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

        TurnoCaja turno = TurnoCaja.builder()
                .fechaApertura(LocalDateTime.now())
                .montoInicial(montoInicial)
                .totalVentas(BigDecimal.ZERO)   // solo inicial
                .totalGastos(BigDecimal.ZERO)
                .estado(EstadoTurno.ABIERTO)
                .usuario(usuario)
                .build();

        return turnoCajaRepository.save(turno);
    }

    // 🧪 SIMULACIÓN DE CIERRE
    public TurnoCaja simularCierre(BigDecimal efectivoContado, Usuario usuario) {

        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo CAJA puede simular cierre");
        }

        TurnoCaja turno = turnoCajaRepository
                .findByEstadoIn(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .orElseThrow(() -> new BadRequestException("No hay turno abierto"));

        // 🔥 RECALCULAR DESDE VENTAS
        BigDecimal totalVentas = ventaRepository
                .sumarTotalPorTurno(turno.getId());

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

    // 🔒 CIERRE DEFINITIVO
    public TurnoCaja cerrarTurno(BigDecimal montoFinal, Usuario usuario) {

        if (!usuario.getRol().getNombre().equals("CAJA")) {
            throw new BadRequestException("Solo CAJA puede cerrar turno");
        }

        TurnoCaja turno = turnoCajaRepository
                .findByEstadoIn(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .orElseThrow(() -> new BadRequestException("No hay turno para cerrar"));

        // 🔥 RECALCULAR OTRA VEZ (OBLIGATORIO)
        BigDecimal totalVentas = ventaRepository
                .sumarTotalPorTurno(turno.getId());

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
}
