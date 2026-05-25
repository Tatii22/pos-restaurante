package com.pos.repository;

import com.pos.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByTelefono(String telefono);

    boolean existsByTelefonoAndIdNot(String telefono, Long id);

    List<Cliente> findAllByOrderByNombreAsc();

    /**
     * Búsqueda full-text liviana: coincidencia parcial en nombre o teléfono.
     * Normaliza a minúsculas para búsqueda case-insensitive.
     * Limitado a 20 resultados para no saturar el dropdown.
     */
    @Query("""
        SELECT c FROM Cliente c
        WHERE c.activo = true
          AND (
               LOWER(c.nombre)   LIKE LOWER(CONCAT('%', :q, '%'))
            OR c.telefono        LIKE CONCAT('%', :q, '%')
          )
        ORDER BY c.nombre ASC
        LIMIT 20
    """)
    List<Cliente> buscarPorTexto(@Param("q") String q);
}
