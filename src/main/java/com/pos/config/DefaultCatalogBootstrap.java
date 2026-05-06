package com.pos.config;

import com.pos.entity.Categoria;
import com.pos.entity.Rol;
import com.pos.entity.TipoGasto;
import com.pos.repository.CategoriaRepository;
import com.pos.repository.RolRepository;
import com.pos.repository.TipoGastoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "app.bootstrap.catalogs.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DefaultCatalogBootstrap implements CommandLineRunner {

    private static final List<String> DEFAULT_ROLES = List.of(
            "ADMIN",
            "CAJA",
            "DOMI"
    );

    private static final List<CategoriaSeed> DEFAULT_CATEGORIAS = List.of(
            new CategoriaSeed("Almuerzos", "Almuerzos y platos principales"),
            new CategoriaSeed("Bebidas", "Bebidas frias, calientes y refrescos"),
            new CategoriaSeed("Postres", "Postres y productos dulces"),
            new CategoriaSeed("Adicionales", "Extras, acompanamientos y complementos")
    );

    private static final List<String> DEFAULT_TIPOS_GASTO = List.of(
            "Alimentos",
            "Bebidas",
            "Insumos y empaques",
            "Nomina",
            "Servicios publicos",
            "Arriendo",
            "Mantenimiento",
            "Transporte",
            "Administrativos",
            "Comisiones / plataformas",
            "Impuestos"
    );

    private final RolRepository rolRepository;
    private final CategoriaRepository categoriaRepository;
    private final TipoGastoRepository tipoGastoRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedCategorias();
        seedTiposGasto();
    }

    private void seedRoles() {
        DEFAULT_ROLES.stream()
                .filter(nombre -> !rolRepository.existsByNombreIgnoreCase(nombre))
                .map(nombre -> Rol.builder().nombre(nombre).build())
                .forEach(rolRepository::save);
        log.info("Catalogo de roles verificado");
    }

    private void seedCategorias() {
        DEFAULT_CATEGORIAS.stream()
                .filter(seed -> !categoriaRepository.existsByNombreIgnoreCase(seed.nombre()))
                .map(seed -> Categoria.builder()
                        .nombre(seed.nombre())
                        .descripcion(seed.descripcion())
                        .activa(true)
                        .build())
                .forEach(categoriaRepository::save);
        log.info("Catalogo de categorias verificado");
    }

    private void seedTiposGasto() {
        DEFAULT_TIPOS_GASTO.stream()
                .filter(nombre -> !tipoGastoRepository.existsByNombreIgnoreCase(nombre))
                .map(nombre -> TipoGasto.builder().nombre(nombre).build())
                .forEach(tipoGastoRepository::save);
        log.info("Catalogo de tipos de gasto verificado");
    }

    private record CategoriaSeed(String nombre, String descripcion) {
    }
}
