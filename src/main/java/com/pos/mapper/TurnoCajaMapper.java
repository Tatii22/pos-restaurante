package com.pos.mapper;


public class TurnoCajaMapper {

    public static TurnoCajaResponseDTO toDTO(TurnoCaja turno) {
        return new TurnoCajaResponseDTO(
                turno.getId(),
                turno.getFechaApertura(),
                turno.getFechaCierre(),
                turno.getMontoInicial(),
                turno.getTotalVentas(),
                turno.getTotalGastos(),
                turno.getEsperado(),
                turno.getFaltante(),
                turno.getEstado(),
                turno.getUsuario().getUsername()
        );
    }
}

