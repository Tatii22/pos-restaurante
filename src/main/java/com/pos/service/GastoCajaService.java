package com.pos.service;

import com.pos.dto.turno.GastoCajaCreateDTO;
import com.pos.entity.*;
import com.pos.exception.BadRequestException;
import com.pos.repository.GastoCajaRepository;
import com.pos.repository.TipoGastoRepository;
import com.pos.repository.TurnoCajaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

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
}
