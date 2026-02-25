package com.pos.dto.configuracion;

public record AdminConfigDTO(
        String negocioNombre,
        String negocioNit,
        String negocioTelefono,
        String negocioDireccion,
        String ticketEncabezado,
        String ticketPie,
        boolean imprimirFacturaAuto,
        boolean imprimirCocinaAuto,
        String tamanoFuenteTicket
) {
}
