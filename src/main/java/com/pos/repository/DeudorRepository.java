package com.pos.repository;

import com.pos.entity.Deudor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeudorRepository extends JpaRepository<Deudor, Long> {

    Optional<Deudor> findByTelefono(String telefono);

    boolean existsByTelefonoAndIdNot(String telefono, Long id);

    List<Deudor> findAllByOrderByNombreAsc();

    /**
     * Búsqueda full-text liviana: coincidencia parcial en nombre o teléfono.
     * Normaliza a minúsculas para búsqueda case-insensitive.
     * Limitado a 20 resultados para no saturar el dropdown.
     */
    @Query("""
        SELECT d FROM Deudor d
        WHERE d.activo = true
          AND (
               LOWER(d.nombre)   LIKE LOWER(CONCAT('%', :q, '%'))
            OR d.telefono        LIKE CONCAT('%', :q, '%')
          )
        ORDER BY d.nombre ASC
        LIMIT 20
    """)
    List<Deudor> buscarPorTexto(@Param("q") String q);
}
