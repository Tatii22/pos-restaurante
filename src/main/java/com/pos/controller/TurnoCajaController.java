package com.pos.controller;

import com.pos.dto.turno.TurnoCajaAperturaDTO;
import com.pos.dto.turno.TurnoCajaResponseDTO;
import com.pos.dto.turno.TurnoCierreDTO;
import com.pos.dto.turno.TurnoSimulacroDTO;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Usuario;
import com.pos.mapper.TurnoCajaMapper;
import com.pos.service.TurnoCajaService;
import com.pos.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/turnos")
@RequiredArgsConstructor
public class TurnoCajaController {

    private final TurnoCajaService turnoCajaService;
    private final UsuarioService usuarioService;

    @PreAuthorize("hasRole('CAJA')")
    @PostMapping("/abrir")
    public ResponseEntity<TurnoCajaResponseDTO> abrirTurno(
            @Valid @RequestBody TurnoCajaAperturaDTO dto,
            Authentication authentication
    ) {
        var turno = turnoCajaService.abrirTurno(
                dto.getMontoInicial(),
                authentication.getName()
        );

        return ResponseEntity.ok(TurnoCajaMapper.toDTO(turno));
    }
    @PostMapping("/simular-cierre")
    @PreAuthorize("hasRole('CAJA')")
    public ResponseEntity<TurnoCajaResponseDTO> simularCierre(
            @Valid @RequestBody TurnoSimulacroDTO dto,
            Authentication auth
    ) {
        Usuario usuario = usuarioService.obtenerPorUsername(auth.getName());
        TurnoCaja turno = turnoCajaService.simularCierre(dto.efectivoContado(), usuario);
        return ResponseEntity.ok(TurnoCajaMapper.toDTO(turno));
    }

    @PostMapping("/cerrar")
    @PreAuthorize("hasRole('CAJA')")
    public ResponseEntity<TurnoCajaResponseDTO> cerrarTurno(
            @Valid @RequestBody TurnoCierreDTO dto,
            Authentication auth
    ) {
        Usuario usuario = usuarioService.obtenerPorUsername(auth.getName());
        TurnoCaja turno = turnoCajaService.cerrarTurno(dto.montoFinal(), usuario);
        return ResponseEntity.ok(TurnoCajaMapper.toDTO(turno));
    }

    @GetMapping("/activo")
    @PreAuthorize("hasAnyRole('CAJA','DOMI')")
    public ResponseEntity<TurnoCajaResponseDTO> obtenerTurnoActivo() {
        TurnoCaja turno = turnoCajaService.obtenerTurnoActivo();
        if (turno == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(TurnoCajaMapper.toDTO(turno));
    }

    @GetMapping("/rango")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TurnoCajaResponseDTO>> listarPorRango(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin,
            Authentication auth
    ) {
        List<TurnoCajaResponseDTO> data = turnoCajaService
                .listarPorRango(fechaInicio, fechaFin, auth.getName())
                .stream()
                .map(TurnoCajaMapper::toDTO)
                .toList();
        return ResponseEntity.ok(data);
    }


}
