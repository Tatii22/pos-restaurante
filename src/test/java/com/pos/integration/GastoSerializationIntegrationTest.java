package com.pos.integration;

import com.pos.entity.EstadoTurno;
import com.pos.entity.Rol;
import com.pos.entity.TipoGasto;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Usuario;
import com.pos.repository.RolRepository;
import com.pos.repository.TipoGastoRepository;
import com.pos.repository.TurnoCajaRepository;
import com.pos.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GastoSerializationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TipoGastoRepository tipoGastoRepository;

    @Autowired
    private TurnoCajaRepository turnoCajaRepository;

    private String adminUsername;
    private String cajaUsername;
    private Long tipoGastoId;
    private Usuario cajaUsuario;

    @BeforeEach
    void setUp() {
        Rol adminRol = rolRepository.findByNombre("ADMIN")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("ADMIN").build()));
        Rol cajaRol = rolRepository.findByNombre("CAJA")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("CAJA").build()));

        adminUsername = "admin_" + UUID.randomUUID().toString().substring(0, 8);
        cajaUsername = "caja_" + UUID.randomUUID().toString().substring(0, 8);

        usuarioRepository.save(Usuario.builder()
                .username(adminUsername)
                .password("secreto-admin")
                .rol(adminRol)
                .activo(true)
                .build());

        cajaUsuario = usuarioRepository.save(Usuario.builder()
                .username(cajaUsername)
                .password("secreto-caja")
                .rol(cajaRol)
                .activo(true)
                .build());

        tipoGastoId = tipoGastoRepository.save(TipoGasto.builder()
                .nombre("Servicios " + UUID.randomUUID().toString().substring(0, 4))
                .build()).getId();
    }

    @Test
    void gastoAdminNoDebeExponerPasswordNiEntidadUsuario() throws Exception {
        mockMvc.perform(post("/api/v1/gastos-admin")
                        .with(user(adminUsername).roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fecha": "2026-04-06",
                                  "descripcion": "Pago proveedor",
                                  "montoEfectivo": 15000,
                                  "montoTransferencia": 10000,
                                  "tipoGastoId": %d
                                }
                                """.formatted(tipoGastoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value(adminUsername))
                .andExpect(jsonPath("$.tipoGasto").exists())
                .andExpect(jsonPath("$.monto").value(25000))
                .andExpect(jsonPath("$.montoEfectivo").value(15000))
                .andExpect(jsonPath("$.montoTransferencia").value(10000))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.rol").doesNotExist());
    }

    @Test
    void gastoCajaDebeExponerDesgloseDePagoSinDatosSensibles() throws Exception {
        turnoCajaRepository.save(TurnoCaja.builder()
                .fechaApertura(LocalDateTime.now())
                .montoInicial(BigDecimal.valueOf(100000))
                .totalVentas(BigDecimal.ZERO)
                .totalGastos(BigDecimal.ZERO)
                .estado(EstadoTurno.ABIERTO)
                .usuario(cajaUsuario)
                .build());

        mockMvc.perform(post("/api/v1/gastos-caja")
                        .with(user(cajaUsername).roles("CAJA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "descripcion": "Compra urgente",
                                  "montoEfectivo": 12000,
                                  "montoTransferencia": 8000,
                                  "tipoGastoId": %d
                                }
                                """.formatted(tipoGastoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valor").value(20000))
                .andExpect(jsonPath("$.montoEfectivo").value(12000))
                .andExpect(jsonPath("$.montoTransferencia").value(8000))
                .andExpect(jsonPath("$.usuario").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }
}
