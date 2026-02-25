package com.pos.service;

import com.pos.dto.configuracion.AdminConfigDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private static final int SINGLETON_ID = 1;

    private static final String DEF_NEGOCIO_NOMBRE = "Restaurant POS";
    private static final String DEF_NEGOCIO_NIT = "";
    private static final String DEF_NEGOCIO_TELEFONO = "";
    private static final String DEF_NEGOCIO_DIRECCION = "";
    private static final String DEF_TICKET_ENCABEZADO = "Gracias por su compra";
    private static final String DEF_TICKET_PIE = "Vuelve pronto";
    private static final boolean DEF_IMPRIMIR_FACTURA_AUTO = true;
    private static final boolean DEF_IMPRIMIR_COCINA_AUTO = true;
    private static final String DEF_TAMANO_FUENTE = "NORMAL";

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    void init() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS admin_configuracion (
                  id INT NOT NULL PRIMARY KEY,
                  negocio_nombre VARCHAR(80) NOT NULL,
                  negocio_nit VARCHAR(30) NOT NULL,
                  negocio_telefono VARCHAR(20) NOT NULL,
                  negocio_direccion VARCHAR(120) NOT NULL,
                  ticket_encabezado VARCHAR(100) NOT NULL,
                  ticket_pie VARCHAR(100) NOT NULL,
                  imprimir_factura_auto BOOLEAN NOT NULL,
                  imprimir_cocina_auto BOOLEAN NOT NULL,
                  tamano_fuente_ticket VARCHAR(10) NOT NULL,
                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);
        ensureSingletonRow();
    }

    public AdminConfigDTO obtener() {
        ensureSingletonRow();
        return jdbcTemplate.query(
                """
                        SELECT negocio_nombre, negocio_nit, negocio_telefono, negocio_direccion,
                               ticket_encabezado, ticket_pie, imprimir_factura_auto,
                               imprimir_cocina_auto, tamano_fuente_ticket
                          FROM admin_configuracion
                         WHERE id = ?
                        """,
                rs -> {
                    if (!rs.next()) {
                        return defaults();
                    }
                    return new AdminConfigDTO(
                            rs.getString("negocio_nombre"),
                            rs.getString("negocio_nit"),
                            rs.getString("negocio_telefono"),
                            rs.getString("negocio_direccion"),
                            rs.getString("ticket_encabezado"),
                            rs.getString("ticket_pie"),
                            rs.getBoolean("imprimir_factura_auto"),
                            rs.getBoolean("imprimir_cocina_auto"),
                            rs.getString("tamano_fuente_ticket")
                    );
                },
                SINGLETON_ID
        );
    }

    public AdminConfigDTO guardar(AdminConfigDTO input) {
        AdminConfigDTO cfg = sanitize(input);
        jdbcTemplate.update(
                """
                        INSERT INTO admin_configuracion (
                          id, negocio_nombre, negocio_nit, negocio_telefono, negocio_direccion,
                          ticket_encabezado, ticket_pie, imprimir_factura_auto,
                          imprimir_cocina_auto, tamano_fuente_ticket
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE
                          negocio_nombre = VALUES(negocio_nombre),
                          negocio_nit = VALUES(negocio_nit),
                          negocio_telefono = VALUES(negocio_telefono),
                          negocio_direccion = VALUES(negocio_direccion),
                          ticket_encabezado = VALUES(ticket_encabezado),
                          ticket_pie = VALUES(ticket_pie),
                          imprimir_factura_auto = VALUES(imprimir_factura_auto),
                          imprimir_cocina_auto = VALUES(imprimir_cocina_auto),
                          tamano_fuente_ticket = VALUES(tamano_fuente_ticket)
                        """,
                SINGLETON_ID,
                cfg.negocioNombre(),
                cfg.negocioNit(),
                cfg.negocioTelefono(),
                cfg.negocioDireccion(),
                cfg.ticketEncabezado(),
                cfg.ticketPie(),
                cfg.imprimirFacturaAuto(),
                cfg.imprimirCocinaAuto(),
                cfg.tamanoFuenteTicket()
        );
        return cfg;
    }

    private void ensureSingletonRow() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_configuracion WHERE id = ?",
                Integer.class,
                SINGLETON_ID
        );
        if (count != null && count > 0) {
            return;
        }
        AdminConfigDTO d = defaults();
        jdbcTemplate.update(
                """
                        INSERT INTO admin_configuracion (
                          id, negocio_nombre, negocio_nit, negocio_telefono, negocio_direccion,
                          ticket_encabezado, ticket_pie, imprimir_factura_auto,
                          imprimir_cocina_auto, tamano_fuente_ticket
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                SINGLETON_ID,
                d.negocioNombre(),
                d.negocioNit(),
                d.negocioTelefono(),
                d.negocioDireccion(),
                d.ticketEncabezado(),
                d.ticketPie(),
                d.imprimirFacturaAuto(),
                d.imprimirCocinaAuto(),
                d.tamanoFuenteTicket()
        );
    }

    private AdminConfigDTO defaults() {
        return new AdminConfigDTO(
                DEF_NEGOCIO_NOMBRE,
                DEF_NEGOCIO_NIT,
                DEF_NEGOCIO_TELEFONO,
                DEF_NEGOCIO_DIRECCION,
                DEF_TICKET_ENCABEZADO,
                DEF_TICKET_PIE,
                DEF_IMPRIMIR_FACTURA_AUTO,
                DEF_IMPRIMIR_COCINA_AUTO,
                DEF_TAMANO_FUENTE
        );
    }

    private AdminConfigDTO sanitize(AdminConfigDTO in) {
        if (in == null) {
            return defaults();
        }
        String fuente = trimToEmpty(in.tamanoFuenteTicket()).toUpperCase();
        if (!"SMALL".equals(fuente) && !"NORMAL".equals(fuente) && !"LARGE".equals(fuente)) {
            fuente = DEF_TAMANO_FUENTE;
        }

        String nombre = trimToEmpty(in.negocioNombre());
        if (nombre.isBlank()) {
            nombre = DEF_NEGOCIO_NOMBRE;
        }

        return new AdminConfigDTO(
                cut(nombre, 80),
                cut(trimToEmpty(in.negocioNit()), 30),
                cut(trimToEmpty(in.negocioTelefono()), 20),
                cut(trimToEmpty(in.negocioDireccion()), 120),
                cut(trimToEmpty(in.ticketEncabezado()), 100),
                cut(trimToEmpty(in.ticketPie()), 100),
                in.imprimirFacturaAuto(),
                in.imprimirCocinaAuto(),
                fuente
        );
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String cut(String value, int maxLen) {
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
}
