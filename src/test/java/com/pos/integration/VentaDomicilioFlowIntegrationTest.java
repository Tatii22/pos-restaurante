package com.pos.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.entity.Categoria;
import com.pos.entity.EstadoTurno;
import com.pos.entity.EstadoVenta;
import com.pos.entity.Producto;
import com.pos.entity.Rol;
import com.pos.entity.TipoVentaProducto;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Usuario;
import com.pos.repository.CategoriaRepository;
import com.pos.repository.ProductoRepository;
import com.pos.repository.RolRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VentaDomicilioFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private String usernameCaja;
    private String usernameDomi;

    @BeforeEach
    void setUp() {
        Rol rolCaja = rolRepository.findByNombre("CAJA")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("CAJA").build()));
        Rol rolDomi = rolRepository.findByNombre("DOMI")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("DOMI").build()));

        usernameCaja = "caja_" + UUID.randomUUID().toString().substring(0, 8);
        usernameDomi = "domi_" + UUID.randomUUID().toString().substring(0, 8);

        Usuario caja = usuarioRepository.save(Usuario.builder()
                .username(usernameCaja)
                .password("123")
                .rol(rolCaja)
                .activo(true)
                .build());

        usuarioRepository.save(Usuario.builder()
                .username(usernameDomi)
                .password("123")
                .rol(rolDomi)
                .activo(true)
                .build());

        Categoria categoria = categoriaRepository.save(Categoria.builder()
                .nombre("Platos " + UUID.randomUUID().toString().substring(0, 5))
                .activa(true)
                .build());

        productoRepository.save(Producto.builder()
                .nombre("Bandeja Paisa")
                .precio(new BigDecimal("20000.00"))
                .activo(true)
                .categoria(categoria)
                .tipoVenta(TipoVentaProducto.SIEMPRE_DISPONIBLE)
                .build());

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
    void flujoDomicilioEnProcesoActualizarImprimirDespachar() throws Exception {
        Long ventaId = crearVentaDomicilioEnProceso();

        mockMvc.perform(put("/api/v1/ventas/{id}/valor-domicilio", ventaId)
                        .with(user(usernameDomi).roles("DOMI"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "valorDomicilio": 3000 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(EstadoVenta.EN_PROCESO.name()))
                .andExpect(jsonPath("$.valorDomicilio").value(3000))
                .andExpect(jsonPath("$.total").value(23000));

        mockMvc.perform(post("/api/v1/ventas/{id}/imprimir-cocina", ventaId)
                        .with(user(usernameDomi).roles("DOMI")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(EstadoVenta.EN_PROCESO.name()));

        mockMvc.perform(post("/api/v1/ventas/{id}/imprimir-factura", ventaId)
                        .with(user(usernameDomi).roles("DOMI")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(EstadoVenta.EN_PROCESO.name()));

        mockMvc.perform(get("/api/v1/ventas")
                        .with(user(usernameCaja).roles("CAJA"))
                        .param("estado", EstadoVenta.EN_PROCESO.name())
                        .param("tipoVenta", "DOMICILIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ventaId));

        mockMvc.perform(post("/api/v1/ventas/{id}/despachar", ventaId)
                        .with(user(usernameCaja).roles("CAJA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "formaPago": "EFECTIVO",
                                  "pagoEfectivo": 23000,
                                  "pagoTransferencia": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(EstadoVenta.DESPACHADA.name()))
                .andExpect(jsonPath("$.total").value(23000));

        MvcResult obtenerResult = mockMvc.perform(get("/api/v1/ventas/{id}", ventaId)
                        .with(user(usernameCaja).roles("CAJA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(EstadoVenta.DESPACHADA.name()))
                .andReturn();

        JsonNode obtenerJson = objectMapper.readTree(obtenerResult.getResponse().getContentAsString());
        assertThat(obtenerJson.get("valorDomicilio").decimalValue())
                .isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(obtenerJson.get("total").decimalValue())
                .isEqualByComparingTo(new BigDecimal("23000.00"));
    }

    @Test
    void domiNoPuedeDespachar() throws Exception {
        Long ventaId = crearVentaDomicilioEnProceso();

        mockMvc.perform(post("/api/v1/ventas/{id}/despachar", ventaId)
                        .with(user(usernameDomi).roles("DOMI"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "formaPago": "EFECTIVO",
                                  "pagoEfectivo": 20000,
                                  "pagoTransferencia": 0
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("Acceso denegado"))
                .andExpect(jsonPath("$.path").value("/api/v1/ventas/" + ventaId + "/despachar"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void noDebePermitirValorDomicilioNegativo() throws Exception {
        Long ventaId = crearVentaDomicilioEnProceso();

        mockMvc.perform(put("/api/v1/ventas/{id}/valor-domicilio", ventaId)
                        .with(user(usernameDomi).roles("DOMI"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "valorDomicilio": -1 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Error de validación"))
                .andExpect(jsonPath("$.path").value("/api/v1/ventas/" + ventaId + "/valor-domicilio"))
                .andExpect(jsonPath("$.fieldErrors.valorDomicilio").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void noDebeImprimirFacturaCuandoNoEstaEnProceso() throws Exception {
        Long ventaId = crearVentaDomicilioEnProceso();

        mockMvc.perform(post("/api/v1/ventas/{id}/despachar", ventaId)
                        .with(user(usernameCaja).roles("CAJA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "formaPago": "EFECTIVO",
                                  "pagoEfectivo": 20000,
                                  "pagoTransferencia": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(EstadoVenta.DESPACHADA.name()));

        mockMvc.perform(post("/api/v1/ventas/{id}/imprimir-factura", ventaId)
                        .with(user(usernameDomi).roles("DOMI")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/v1/ventas/" + ventaId + "/imprimir-factura"));
    }

    @Test
    void noDebePermitirDespacharSinPagoSuficiente() throws Exception {
        Long ventaId = crearVentaDomicilioEnProceso();

        mockMvc.perform(post("/api/v1/ventas/{id}/despachar", ventaId)
                        .with(user(usernameCaja).roles("CAJA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "formaPago": "EFECTIVO",
                                  "pagoEfectivo": 10000,
                                  "pagoTransferencia": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("El pago es insuficiente para despachar la venta"));
    }

    private Long crearVentaDomicilioEnProceso() throws Exception {
        Producto producto = productoRepository.findAll().get(0);

        String payloadCrear = """
                {
                  "tipoVenta": "DOMICILIO",
                  "formaPago": "EFECTIVO",
                  "clienteNombre": "Juan Perez",
                  "telefono": "3001234567",
                  "direccion": "Calle 123 # 45-67",
                  "detalles": [
                    {
                      "productoId": %d,
                      "cantidad": 1,
                      "observacion": "Sin cebolla"
                    }
                  ]
                }
                """.formatted(producto.getId());

        MvcResult crearResult = mockMvc.perform(post("/api/v1/ventas")
                        .with(user(usernameDomi).roles("DOMI"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadCrear))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value(EstadoVenta.EN_PROCESO.name()))
                .andExpect(jsonPath("$.tipoVenta").value("DOMICILIO"))
                .andExpect(jsonPath("$.clienteNombre").value("Juan Perez"))
                .andReturn();

        JsonNode crearJson = objectMapper.readTree(crearResult.getResponse().getContentAsString());
        return crearJson.get("id").asLong();
    }
}
