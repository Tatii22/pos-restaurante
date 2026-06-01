package com.pos.service;

import com.pos.entity.EstadoTurno;
import com.pos.repository.TurnoCajaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FechaOperativaService {

    private final TurnoCajaRepository turnoCajaRepository;

    public LocalDate obtenerFechaOperativa() {
        return turnoCajaRepository
                .findByEstado(EstadoTurno.ABIERTO)
                .map(turno -> turno.getFechaApertura().toLocalDate())
                .orElse(LocalDate.now());
    }
}
