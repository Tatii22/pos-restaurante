package com.pos.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.entity.CanalVenta;
import com.pos.entity.Categoria;
import com.pos.entity.EstadoEntregaCaja;
import com.pos.entity.EstadoTurno;
import com.pos.entity.EstadoVenta;
import com.pos.entity.InventarioDiario;
import com.pos.entity.MenuDiario;
import com.pos.entity.MovimientoFinanciero;
import com.pos.entity.Producto;
import com.pos.entity.Rol;
import com.pos.entity.TipoVentaProducto;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Usuario;
import com.pos.repository.CategoriaRepository;
import com.pos.repository.InventarioDiarioRepository;
import com.pos.repository.MenuDiarioRepository;
import com.pos.repository.MovimientoFinancieroRepository;
import com.pos.repository.ProductoRepository;
import com.pos.repository.RolRepository;
import com.pos.repository.TurnoCajaRepository;
import com.pos.repository.UsuarioRepository;
import com.pos.repository.VentaRepository;
import com.pos.service.FechaOperativaService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VentaMeseroIntegrationTest {

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

    @Autowired
    private MenuDiarioRepository menuDiarioRepository;

    @Autowired
    private InventarioDiarioRepository inventarioDiarioRepository;

    @Autowired
    private MovimientoFinancieroRepository movimientoFinancieroRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private FechaOperativaService fechaOperativaService;

    private String usernameCaja;
    private String usernameMesero;
    private Usuario caja;
    private Usuario mesero;
    private Long productoMenuId;
    private Long productoLibreId;
    private MenuDiario menuActivo;

    @BeforeEach
    void setUp() {
        Rol rolCaja = rolRepository.findByNombre("CAJA")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("CAJA").build()));
        Rol rolMesero = rolRepository.findByNombre("MESERO")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("MESERO").build()));

        usernameCaja = "caja_" + UUID.randomUUID().toString().substring(0, 8);
        usernameMesero = "mesero_" + UUID.randomUUID().toString().substring(0, 8);

        caja = usuarioRepository.save(Usuario.builder()
                .username(usernameCaja)
                .password("123")
                .rol(rolCaja)
                .activo(true)
                .build());

        mesero = usuarioRepository.save(Usuario.builder()
                .username(usernameMesero)
                .password("123")
                .rol(rolMesero)
                .activo(true)
                .build());

        Categoria categoria = categoriaRepository.save(Categoria.builder()
                .nombre("Menu " + UUID.randomUUID().toString().substring(0, 5))
                .activa(true)
                .build());

        Producto prodMenu = productoRepository.save(Producto.builder()
                .nombre("Almuerzo ejecutivo")
                .precio(new BigDecimal("15000.00"))
                .activo(true)
                .categoria(categoria)
                .tipoVenta(TipoVentaProducto.MENU_DIARIO)
                .build());
        productoMenuId = prodMenu.getId();

        Producto prodLibre = productoRepository.save(Producto.builder()
                .nombre("Gaseosa")
                .precio(new BigDecimal("5000.00"))
                .activo(true)
                .categoria(categoria)
                .tipoVenta(TipoVentaProducto.SIEMPRE_DISPONIBLE)
                .build());
        productoLibreId = prodLibre.getId();

        LocalDate fechaOperativa = fechaOperativaService.obtenerFechaOperativa();
        menuActivo = menuDiarioRepository.save(MenuDiario.builder()
                .fecha(fechaOperativa)
                .usuario(caja)
                .activo(true)
                .build());

        inventarioDiarioRepository.save(InventarioDiario.builder()
                .fecha(fechaOperativa)
                .producto(prodMenu)
                .menuDiario(menuActivo)
                .stockInicial(20)
                .stockActual(20)
                .agotado(false)
                .build());

        turnoCajaRepository.save(TurnoCaja.builder()
                .fechaApertura(LocalDateTime.now())
                .montoInicial(new BigDecimal("100000.00"))
                .totalVentas(BigDecimal.ZERO)
                .totalGastos(BigDecimal.ZERO)
                .estado(EstadoTurno.ABIERTO)
                .usuario(caja)
                .numeroTurno(1)
                .build());
    }

    /* ===================== CASO 1 ===================== */

    @Test
    void ventaMeseroEfectivoGeneraPendienteMovimientoFinancieroYDescuentaInventario() throws Exception {
        Integer stockInicial = inventarioDiarioRepository
                .findByProductoAndMenuDiario(
                        productoRepository.findById(productoMenuId).orElseThrow(),
                        menuActivo)
                .orElseThrow()
                .getStockActual();

        MvcResult result = mockMvc.perform(post("/api/v1/ventas")
                        .with(user(usernameMesero).roles("MESERO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoVenta": "LOCAL",
                                  "formaPago": "EFECTIVO",
                                  "pagoEfectivo": 30000,
                                  "pagoTransferencia": 0,
                                  "detalles": [
                                    {
                                      "productoId": %d,
                                      "cantidad": 2
                                    }
                                  ]
                                }
                                """.formatted(productoMenuId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canalVenta").value(CanalVenta.MESERO.name()))
                .andExpect(jsonPath("$.estadoEntregaCaja").value(EstadoEntregaCaja.PENDIENTE.name()))
                .andExpect(jsonPath("$.estado").value(EstadoVenta.EN_PROCESO.name()))
                .andExpect(jsonPath("$.total").value(30000))
                .andExpect(jsonPath("$.usuario").value(usernameMesero))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Long ventaId = body.get("id").asLong();

        var venta = ventaRepository.findById(ventaId).orElseThrow();
        assertThat(venta.getCanalVenta()).isEqualTo(CanalVenta.MESERO);
        assertThat(venta.getEstadoEntregaCaja()).isEqualTo(EstadoEntregaCaja.PENDIENTE);
        assertThat(venta.getUsuario().getUsername()).isEqualTo(usernameMesero);

        List<MovimientoFinanciero> movimientos = movimientoFinancieroRepository
                .findByTurnoOrderByFechaAsc(venta.getTurno());
        assertThat(movimientos)
                .as("Debe existir un movimiento financiero para la venta de mesero")
                .isNotEmpty();
        assertThat(movimientos)
                .anyMatch(m -> m.getVenta() != null
                        && m.getVenta().getId().equals(ventaId)
                        && m.getMonto().compareTo(new BigDecimal("30000.00")) == 0);

        Integer stockFinal = inventarioDiarioRepository
                .findByProductoAndMenuDiario(
                        productoRepository.findById(productoMenuId).orElseThrow(),
                        menuActivo)
                .orElseThrow()
                .getStockActual();
        assertThat(stockFinal)
                .as("El inventario del menu diario debe haberse descontado")
                .isEqualTo(stockInicial - 2);
    }

    /* ===================== CASO 2 ===================== */

    @Test
    void ventaMeseroTransferenciaNacePendienteParaValidacionCaja() throws Exception {
        mockMvc.perform(post("/api/v1/ventas")
                        .with(user(usernameMesero).roles("MESERO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoVenta": "LOCAL",
                                  "formaPago": "TRANSFERENCIA",
                                  "pagoEfectivo": 0,
                                  "pagoTransferencia": 10000,
                                  "detalles": [
                                    {
                                      "productoId": %d,
                                      "cantidad": 2
                                    }
                                  ]
                                }
                                """.formatted(productoLibreId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canalVenta").value(CanalVenta.MESERO.name()))
                .andExpect(jsonPath("$.estadoEntregaCaja").value(EstadoEntregaCaja.PENDIENTE.name()))
                .andExpect(jsonPath("$.pagoTransferencia").value(10000))
                .andExpect(jsonPath("$.pagoEfectivo").value(0));
    }

    /* ===================== CASO 3 ===================== */

    @Test
    void ventaMeseroPagoMixtoGeneraPendientePorEfectivo() throws Exception {
        mockMvc.perform(post("/api/v1/ventas")
                        .with(user(usernameMesero).roles("MESERO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoVenta": "LOCAL",
                                  "formaPago": "EFECTIVO",
                                  "pagoEfectivo": 10000,
                                  "pagoTransferencia": 5000,
                                  "detalles": [
                                    {
                                      "productoId": %d,
                                      "cantidad": 3
                                    }
                                  ]
                                }
                                """.formatted(productoLibreId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canalVenta").value(CanalVenta.MESERO.name()))
                .andExpect(jsonPath("$.estadoEntregaCaja").value(EstadoEntregaCaja.PENDIENTE.name()))
                .andExpect(jsonPath("$.pagoEfectivo").value(10000))
                .andExpect(jsonPath("$.pagoTransferencia").value(5000));
    }

    /* ===================== CASO 4 ===================== */

    @Test
    void cajaPuedeConfirmarEntregaDeMeseroYPasaAEntregado() throws Exception {
        MvcResult crear = mockMvc.perform(post("/api/v1/ventas")
                        .with(user(usernameMesero).roles("MESERO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoVenta": "LOCAL",
                                  "formaPago": "EFECTIVO",
                                  "pagoEfectivo": 5000,
                                  "pagoTransferencia": 0,
                                  "detalles": [
                                    { "productoId": %d, "cantidad": 1 }
                                  ]
                                }
                                """.formatted(productoLibreId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoEntregaCaja").value(EstadoEntregaCaja.PENDIENTE.name()))
                .andReturn();

        JsonNode body = objectMapper.readTree(crear.getResponse().getContentAsString());
        Long ventaId = body.get("id").asLong();

        mockMvc.perform(post("/api/v1/ventas/confirmar-entrega-caja")
                        .with(user(usernameCaja).roles("CAJA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + ventaId + "]"))
                .andExpect(status().isOk());

        var venta = ventaRepository.findById(ventaId).orElseThrow();
        assertThat(venta.getEstadoEntregaCaja()).isEqualTo(EstadoEntregaCaja.ENTREGADO);
    }

    /* ===================== CASO 5 ===================== */

    @Test
    void cierreTurnoBloqueaPorVentaEnProcesoYPorEntregaCajaPendiente() throws Exception {
        MvcResult crear = mockMvc.perform(post("/api/v1/ventas")
                        .with(user(usernameMesero).roles("MESERO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoVenta": "LOCAL",
                                  "formaPago": "EFECTIVO",
                                  "pagoEfectivo": 10000,
                                  "pagoTransferencia": 0,
                                  "detalles": [
                                    { "productoId": %d, "cantidad": 2 }
                                  ]
                                }
                                """.formatted(productoLibreId)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(crear.getResponse().getContentAsString());
        Long ventaId = body.get("id").asLong();

        mockMvc.perform(post("/api/v1/turnos/cerrar")
                        .with(user(usernameCaja).roles("CAJA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "efectivoContado": 100000, "transferenciasVerificadas": 0 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("ventas en proceso")));

        mockMvc.perform(post("/api/v1/ventas/confirmar-entrega-caja")
                        .with(user(usernameCaja).roles("CAJA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + ventaId + "]"))
                .andExpect(status().isOk());

        var ventaPersistida = ventaRepository.findById(ventaId).orElseThrow();
        ventaPersistida.setEstado(EstadoVenta.DESPACHADA);
        ventaRepository.save(ventaPersistida);

        mockMvc.perform(get("/api/v1/ventas/pendientes-meseros")
                        .with(user(usernameCaja).roles("CAJA")))
                .andExpect(status().isOk());
    }

    /* ===================== Tarea 1: filtro "Mis Pedidos" ===================== */

    @Test
    void misPedidosFiltraPorMeseroAutenticado() throws Exception {
        String usernameOtro = "mesero2_" + UUID.randomUUID().toString().substring(0, 8);
        Rol rolMesero = rolRepository.findByNombre("MESERO").orElseThrow();
        Usuario otroMesero = usuarioRepository.save(Usuario.builder()
                .username(usernameOtro)
                .password("123")
                .rol(rolMesero)
                .activo(true)
                .build());

        mockMvc.perform(post("/api/v1/ventas")
                        .with(user(usernameOtro).roles("MESERO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ventaBody(productoLibreId, 1, 5000, 0)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/ventas")
                        .with(user(usernameMesero).roles("MESERO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ventaBody(productoLibreId, 1, 5000, 0)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/ventas/pendientes-meseros")
                        .with(user(usernameMesero).roles("MESERO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].usuario").value(usernameMesero));

        mockMvc.perform(get("/api/v1/ventas/pendientes-meseros")
                        .with(user(usernameOtro).roles("MESERO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].usuario").value(usernameOtro));

        mockMvc.perform(get("/api/v1/ventas/pendientes-meseros")
                        .with(user(usernameCaja).roles("CAJA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    /* ===================== CASO 1: MESERO ve venta por transferencia ===================== */

    @Test
    void caso1_meseroVeVentaTransferenciaEnMisPedidos() throws Exception {
        Long ventaId = crearVentaComoMesero(usernameMesero, 0, 5000);

        var venta = ventaRepository.findById(ventaId).orElseThrow();
        assertThat(venta.getEstadoEntregaCaja())
                .as("Toda venta MESERO nace PENDIENTE, incluso si es por transferencia")
                .isEqualTo(EstadoEntregaCaja.PENDIENTE);

        mockMvc.perform(get("/api/v1/ventas/pendientes-meseros")
                        .with(user(usernameMesero).roles("MESERO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ventaId))
                .andExpect(jsonPath("$[0].estadoEntregaCaja").value(EstadoEntregaCaja.PENDIENTE.name()))
                .andExpect(jsonPath("$[0].usuario").value(usernameMesero));
    }

    /* ===================== CASO 2: MESERO ve venta en efectivo ===================== */

    @Test
    void caso2_meseroVeVentaEfectivoEnMisPedidos() throws Exception {
        Long ventaId = crearVentaComoMesero(usernameMesero, 5000, 0);

        var venta = ventaRepository.findById(ventaId).orElseThrow();
        assertThat(venta.getEstadoEntregaCaja()).isEqualTo(EstadoEntregaCaja.PENDIENTE);

        mockMvc.perform(get("/api/v1/ventas/pendientes-meseros")
                        .with(user(usernameMesero).roles("MESERO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ventaId))
                .andExpect(jsonPath("$[0].estadoEntregaCaja").value(EstadoEntregaCaja.PENDIENTE.name()))
                .andExpect(jsonPath("$[0].usuario").value(usernameMesero));
    }

    /* ===================== CASO 3: CAJA confirma entrega y MESERO sigue viendo ===================== */

    @Test
    void caso3_meseroSigueViendoVentaTrasConfirmarEntregaCaja() throws Exception {
        Long ventaId = crearVentaComoMesero(usernameMesero, 5000, 0);

        mockMvc.perform(post("/api/v1/ventas/confirmar-entrega-caja")
                        .with(user(usernameCaja).roles("CAJA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + ventaId + "]"))
                .andExpect(status().isOk());

        var venta = ventaRepository.findById(ventaId).orElseThrow();
        assertThat(venta.getEstadoEntregaCaja()).isEqualTo(EstadoEntregaCaja.ENTREGADO);

        mockMvc.perform(get("/api/v1/ventas/pendientes-meseros")
                        .with(user(usernameMesero).roles("MESERO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(ventaId))
                .andExpect(jsonPath("$[0].estadoEntregaCaja").value(EstadoEntregaCaja.ENTREGADO.name()));
    }

    /* ===================== CASO 4: aislamiento entre meseros ===================== */

    @Test
    void caso4_meseroPedroNoVePedidosDeJuan() throws Exception {
        String usernamePedro = "mesero3_" + UUID.randomUUID().toString().substring(0, 8);
        Rol rolMesero = rolRepository.findByNombre("MESERO").orElseThrow();
        usuarioRepository.save(Usuario.builder()
                .username(usernamePedro)
                .password("123")
                .rol(rolMesero)
                .activo(true)
                .build());

        Long ventaJuanEfectivo = crearVentaComoMesero(usernameMesero, 5000, 0);
        Long ventaJuanTransferencia = crearVentaComoMesero(usernameMesero, 0, 5000);
        crearVentaComoMesero(usernamePedro, 5000, 0);

        mockMvc.perform(get("/api/v1/ventas/pendientes-meseros")
                        .with(user(usernamePedro).roles("MESERO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].usuario").value(usernamePedro))
                .andExpect(jsonPath("$[?(@.id == " + ventaJuanEfectivo + ")]").isEmpty())
                .andExpect(jsonPath("$[?(@.id == " + ventaJuanTransferencia + ")]").isEmpty());

        mockMvc.perform(get("/api/v1/ventas/pendientes-meseros")
                        .with(user(usernameMesero).roles("MESERO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].usuario").value(usernameMesero));
    }

    /* ===================== Tarea CAJA: ve ventas de multiples meseros ===================== */

    @Test
    void cajaVeTodasLasVentasDeTodosLosMeserosIncluyendoEntregadas() throws Exception {
        String usernamePedro = "mesero_caja_" + UUID.randomUUID().toString().substring(0, 8);
        Rol rolMesero = rolRepository.findByNombre("MESERO").orElseThrow();
        usuarioRepository.save(Usuario.builder()
                .username(usernamePedro)
                .password("123")
                .rol(rolMesero)
                .activo(true)
                .build());

        Long ventaJuanEfectivo = crearVentaComoMesero(usernameMesero, 5000, 0);
        Long ventaJuanTransferencia = crearVentaComoMesero(usernameMesero, 0, 5000);
        Long ventaPedroEfectivo = crearVentaComoMesero(usernamePedro, 5000, 0);
        crearVentaComoMesero(usernamePedro, 0, 5000);

        mockMvc.perform(post("/api/v1/ventas/confirmar-entrega-caja")
                        .with(user(usernameCaja).roles("CAJA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + ventaJuanEfectivo + "," + ventaPedroEfectivo + "]"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/ventas/pendientes-meseros")
                        .with(user(usernameCaja).roles("CAJA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[?(@.usuario == '" + usernameMesero + "')]").exists())
                .andExpect(jsonPath("$[?(@.usuario == '" + usernamePedro + "')]").exists())
                .andExpect(jsonPath("$[?(@.id == " + ventaJuanEfectivo + " && @.estadoEntregaCaja == 'ENTREGADO')]").exists())
                .andExpect(jsonPath("$[?(@.id == " + ventaJuanTransferencia + " && @.estadoEntregaCaja == 'PENDIENTE')]").exists())
                .andExpect(jsonPath("$[?(@.id == " + ventaPedroEfectivo + " && @.estadoEntregaCaja == 'ENTREGADO')]").exists());
    }

    private Long crearVentaComoMesero(String username, int efectivo, int transferencia) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/ventas")
                        .with(user(username).roles("MESERO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ventaBody(productoLibreId, 1, efectivo, transferencia)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private String ventaBody(Long productoId, int cantidad, int efectivo, int transferencia) {
        return """
                {
                  "tipoVenta": "LOCAL",
                  "formaPago": "%s",
                  "pagoEfectivo": %d,
                  "pagoTransferencia": %d,
                  "detalles": [
                    { "productoId": %d, "cantidad": %d }
                  ]
                }
                """.formatted(
                        transferencia > 0 && efectivo == 0 ? "TRANSFERENCIA" : "EFECTIVO",
                        efectivo,
                        transferencia,
                        productoId,
                        cantidad);
    }
}
