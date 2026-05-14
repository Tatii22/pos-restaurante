package com.pos.repository;

import com.pos.entity.GastoAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface GastoAdminRepository extends JpaRepository<GastoAdmin, Long> {

    List<GastoAdmin> findByFecha(LocalDate fecha);

    List<GastoAdmin> findByTipoId(Long tipoId);

    List<GastoAdmin> findByFechaBetween(
            LocalDate fechaInicio,
            LocalDate fechaFin
    );
}
