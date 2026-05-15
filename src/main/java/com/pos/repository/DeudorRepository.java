package com.pos.repository;

import com.pos.entity.Deudor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeudorRepository extends JpaRepository<Deudor, Long> {
    Optional<Deudor> findByTelefono(String telefono);
    boolean existsByTelefonoAndIdNot(String telefono, Long id);
    List<Deudor> findAllByOrderByNombreAsc();
}
