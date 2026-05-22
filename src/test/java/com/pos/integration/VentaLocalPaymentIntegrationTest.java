package com.pos.integration;

import com.pos.entity.Categoria;
import com.pos.entity.EstadoTurno;
import com.pos.entity.Rol;
import com.pos.entity.TipoVentaProducto;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Usuario;
import com.pos.repository.CategoriaRepository;
import com.pos.repository.ProductoRepository;
import com.pos.repository.RolRepository;
import com.pos.repository.TurnoCajaRepository;
import com.pos.repository.UsuarioRepository;
import com.pos.repository.VentaPagoDetalleRepository;
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
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VentaLocalPaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private TurnoCajaRepository turnoCajaRepository;

    @Autowired
    private VentaPagoDetalleRepository ventaPagoDetalleRepository;

    private String usernameCaja;
    private Long productoId;

    @BeforeEach
    void setUp() {
        Rol rolCaja = rolRepository.findByNombre("CAJA")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("CAJA").build()));

        usernameCaja = "caja_" + UUID.randomUUID().toString().substring(0, 8);

        Usuario caja = usuarioRepository.save(Usuario.builder()
                .username(usernameCaja)
                .password("123")
                .rol(rolCaja)
                .activo(true)
                .build());

        Categoria categoria = categoriaRepository.save(Categoria.builder()
                .nombre("Bebidas " + UUID.randomUUID().toString().substring(0, 5))
                .activa(true)
                .build());

        productoId = productoRepository.save(com.pos.entity.Producto.builder()
                .nombre("Jugo natural")
                .precio(new BigDecimal("12000.00"))
                .activo(true)
                .categoria(categoria)
                .tipoVenta(TipoVentaProducto.SIEMPRE_DISPONIBLE)
                .build()).getId();

        turnoCajaRepository.save(TurnoCaja.builder()
                .fechaApertura(LocalDateTime.now())
                .montoInicial(new BigDecimal("100000.00"))
                .totalVentas(BigDecimal.ZERO)
                .totalGastos(BigDecimal.ZERO)
                .estado(EstadoTurno.ABIERTO)
                .usuario(caja)
                .build());
    }

    @Test
    void noDebePermitirVentaLocalConPagoInsuficiente() throws Exception {
        mockMvc.perform(post("/api/v1/ventas")
                        .with(user(usernameCaja).roles("CAJA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoVenta": "LOCAL",
                                  "formaPago": "EFECTIVO",
                                  "pagoEfectivo": 5000,
                                  "pagoTransferencia": 0,
                                  "detalles": [
                                    {
                                      "productoId": %d,
                                      "cantidad": 1,
                                      "observacion": "Sin hielo"
                                    }
                                  ]
                                }
                                """.formatted(productoId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("El pago es insuficiente para registrar la venta"));
    }

    @Test
    void pagoEnEfectivoConCambioSoloContabilizaTotalDeVenta() throws Exception {
        var result = mockMvc.perform(post("/api/v1/ventas")
                        .with(user(usernameCaja).roles("CAJA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoVenta": "LOCAL",
                                  "formaPago": "EFECTIVO",
                                  "pagoEfectivo": 50000,
                                  "pagoTransferencia": 0,
                                  "detalles": [
                                    {
                                      "productoId": %d,
                                      "cantidad": 1
                                    }
                                  ]
                                }
                                """.formatted(productoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(12000))
                .andExpect(jsonPath("$.pagoEfectivo").value(12000))
                .andReturn();

        Number ventaIdRaw = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        Long ventaId = ventaIdRaw.longValue();
        var pago = ventaPagoDetalleRepository.findById(ventaId).orElseThrow();
        assertThat(pago.getPagoEfectivo()).isEqualByComparingTo("12000.00");
        assertThat(pago.getRecibidoEfectivo()).isEqualByComparingTo("50000.00");
        assertThat(pago.getCambioEfectivo()).isEqualByComparingTo("38000.00");
    }

    @Test
    void transferenciaNoAumentaEfectivoEsperadoEnCierre() throws Exception {
        mockMvc.perform(post("/api/v1/ventas")
                        .with(user(usernameCaja).roles("CAJA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoVenta": "LOCAL",
                                  "formaPago": "TRANSFERENCIA",
                                  "pagoEfectivo": 0,
                                  "pagoTransferencia": 12000,
                                  "detalles": [
                                    {
                                      "productoId": %d,
                                      "cantidad": 1
                                    }
                                  ]
                                }
                                """.formatted(productoId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/turnos/cerrar")
                        .with(user(usernameCaja).roles("CAJA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "montoFinal": 100000 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esperado").value(100000))
                .andExpect(jsonPath("$.faltante").value(0));
    }
}
