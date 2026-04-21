package com.pos.integration;

import com.pos.entity.Categoria;
import com.pos.entity.EstadoTurno;
import com.pos.entity.InventarioDiario;
import com.pos.entity.MenuDiario;
import com.pos.entity.Rol;
import com.pos.entity.TipoVentaProducto;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Usuario;
import com.pos.repository.CategoriaRepository;
import com.pos.repository.InventarioDiarioRepository;
import com.pos.repository.MenuDiarioRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TurnoCatalogoInventarioIntegrationTest {

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
    private MenuDiarioRepository menuDiarioRepository;

    @Autowired
    private InventarioDiarioRepository inventarioDiarioRepository;

    @Autowired
    private TurnoCajaRepository turnoCajaRepository;

    private String usernameCaja;
    private Long productoMenuId;
    private Usuario caja;

    @BeforeEach
    void setUp() {
        Rol rolCaja = rolRepository.findByNombre("CAJA")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("CAJA").build()));

        usernameCaja = "caja_" + UUID.randomUUID().toString().substring(0, 8);

        caja = usuarioRepository.save(Usuario.builder()
                .username(usernameCaja)
                .password("123")
                .rol(rolCaja)
                .activo(true)
                .build());

        Categoria categoria = categoriaRepository.save(Categoria.builder()
                .nombre("Almuerzos " + UUID.randomUUID().toString().substring(0, 5))
                .activa(true)
                .build());

        productoMenuId = productoRepository.save(com.pos.entity.Producto.builder()
                .nombre("Almuerzo ejecutivo")
                .precio(new BigDecimal("15000.00"))
                .activo(true)
                .categoria(categoria)
                .tipoVenta(TipoVentaProducto.MENU_DIARIO)
                .build()).getId();
    }

    @Test
    void abrirTurnoNoDebeBorrarInventarioInicialNiOcultarMenuEnVentas() throws Exception {
        mockMvc.perform(post("/api/v1/menu-diario")
                        .with(user(usernameCaja).roles("CAJA")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/inventario-diario")
                        .with(user(usernameCaja).roles("CAJA"))
                        .param("productoId", String.valueOf(productoMenuId))
                        .param("stockInicial", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockInicial").value(12))
                .andExpect(jsonPath("$.stockActual").value(12));

        mockMvc.perform(post("/api/v1/turnos/abrir")
                        .with(user(usernameCaja).roles("CAJA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "montoInicial": 100000
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/inventario-diario")
                        .with(user(usernameCaja).roles("CAJA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productoId").value(productoMenuId.intValue()))
                .andExpect(jsonPath("$[0].stockActual").value(12));

        mockMvc.perform(get("/api/v1/ventas/catalogo-hoy")
                        .with(user(usernameCaja).roles("CAJA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menuDiario[0].id").value(productoMenuId.intValue()))
                .andExpect(jsonPath("$.menuDiario[0].nombre").value("Almuerzo ejecutivo"))
                .andExpect(jsonPath("$.menuDiario[0].agotado").value(false));
    }

    @Test
    void turnoAbiertoDebeSeguirUsandoLaFechaDelTurnoAunqueCambieElDiaDelSistema() throws Exception {
        LocalDate fechaTurno = LocalDate.now().minusDays(1);

        MenuDiario menu = menuDiarioRepository.save(MenuDiario.builder()
                .fecha(fechaTurno)
                .usuario(caja)
                .activo(true)
                .build());

        inventarioDiarioRepository.save(InventarioDiario.builder()
                .fecha(fechaTurno)
                .producto(productoRepository.findById(productoMenuId).orElseThrow())
                .menuDiario(menu)
                .stockInicial(8)
                .stockActual(8)
                .agotado(false)
                .build());

        turnoCajaRepository.save(TurnoCaja.builder()
                .fechaApertura(LocalDateTime.now().minusDays(1))
                .montoInicial(new BigDecimal("100000.00"))
                .totalVentas(BigDecimal.ZERO)
                .totalGastos(BigDecimal.ZERO)
                .estado(EstadoTurno.ABIERTO)
                .usuario(caja)
                .build());

        mockMvc.perform(get("/api/v1/inventario-diario")
                        .with(user(usernameCaja).roles("CAJA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productoId").value(productoMenuId.intValue()))
                .andExpect(jsonPath("$[0].stockActual").value(8));

        mockMvc.perform(get("/api/v1/ventas/catalogo-hoy")
                        .with(user(usernameCaja).roles("CAJA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menuDiario[0].id").value(productoMenuId.intValue()))
                .andExpect(jsonPath("$.menuDiario[0].nombre").value("Almuerzo ejecutivo"));
    }
}
