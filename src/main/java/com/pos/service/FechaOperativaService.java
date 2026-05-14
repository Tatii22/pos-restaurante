package com.pos.service;



@Service
@RequiredArgsConstructor
public class FechaOperativaService {

    private final TurnoCajaRepository turnoCajaRepository;

    public LocalDate obtenerFechaOperativa() {
        return turnoCajaRepository
                .findByEstadoIn(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .map(turno -> turno.getFechaApertura().toLocalDate())
                .orElse(LocalDate.now());
    }
}

