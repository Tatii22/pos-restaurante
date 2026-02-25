package com.pos.service;

import com.pos.entity.MenuDiario;
import com.pos.entity.Usuario;
import com.pos.exception.BadRequestException;
import com.pos.repository.InventarioDiarioRepository;
import com.pos.repository.MenuDiarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MenuDiarioService {

    private final MenuDiarioRepository menuDiarioRepository;
    private final InventarioDiarioRepository inventarioDiarioRepository;

    @Transactional
    public MenuDiario crearMenuHoy(Usuario usuario) {
        LocalDate hoy = LocalDate.now();

        // Idempotente por dia: si ya existe registro (activo o inactivo), se reutiliza.
        MenuDiario menuExistente = menuDiarioRepository.findByFecha(hoy).orElse(null);
        if (menuExistente != null) {
            // Si se reutiliza el menu del dia, se limpia inventario previo para evitar
            // errores "Producto ya existe" al iniciar un nuevo turno.
            inventarioDiarioRepository.deleteByMenuDiario(menuExistente);
            menuExistente.setActivo(true);
            menuExistente.setUsuario(usuario);
            return menuDiarioRepository.save(menuExistente);
        }

        MenuDiario menu = MenuDiario.builder()
                .fecha(hoy)
                .usuario(usuario)
                .activo(true)
                .build();

        return menuDiarioRepository.save(menu);
    }

    public MenuDiario obtenerMenuActivo() {
        return menuDiarioRepository
                .findByFechaAndActivoTrue(LocalDate.now())
                .orElseThrow(() -> new BadRequestException("No hay menu activo hoy"));
    }
}
