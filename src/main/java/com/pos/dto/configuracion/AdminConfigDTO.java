package com.pos.dto.configuracion;

import jakarta.validation.constraints.Size;

public record AdminConfigDTO(
        @Size(max = 80, message = "El nombre del negocio no puede superar 80 caracteres")
        String negocioNombre,
        @Size(max = 30, message = "El NIT no puede superar 30 caracteres")
        String negocioNit,
        @Size(max = 20, message = "El telefono no puede superar 20 caracteres")
        String negocioTelefono,
        @Size(max = 120, message = "La direccion no puede superar 120 caracteres")
        String negocioDireccion,
        @Size(max = 100, message = "El encabezado del ticket no puede superar 100 caracteres")
        String ticketEncabezado,
        @Size(max = 100, message = "El pie del ticket no puede superar 100 caracteres")
        String ticketPie,
        boolean imprimirFacturaAuto,
        boolean imprimirCocinaAuto,
        @Size(max = 10, message = "El tamanio de fuente no puede superar 10 caracteres")
        String tamanoFuenteTicket
) {
}
