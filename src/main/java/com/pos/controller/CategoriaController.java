package com.pos.controller;

import com.pos.dto.categoria.CategoriaCreateDTO;
import com.pos.dto.categoria.CategoriaResponseDTO;
import com.pos.dto.categoria.CategoriaUpdateDTO;
import com.pos.entity.Categoria;
import com.pos.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categorias")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CategoriaController {

    private final CategoriaService categoriaService;

    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> crear(
            @Valid @RequestBody CategoriaCreateDTO dto) {

        Categoria categoria = new Categoria();
        categoria.setNombre(dto.getNombre());
        categoria.setActiva(dto.getActiva());

        Categoria guardada = categoriaService.crear(categoria);

        return ResponseEntity.ok(
                new CategoriaResponseDTO(
                        guardada.getId(),
                        guardada.getNombre(),
                        guardada.getActiva()
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<CategoriaResponseDTO>> listar() {

        List<CategoriaResponseDTO> lista =
                categoriaService.listar()
                        .stream()
                        .map(c -> new CategoriaResponseDTO(
                                c.getId(),
                                c.getNombre(),
                                c.getActiva()
                        ))
                        .toList();

        return ResponseEntity.ok(lista);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaUpdateDTO dto) {

        Categoria categoria = new Categoria();
        categoria.setNombre(dto.getNombre());
        categoria.setActiva(dto.getActiva());

        Categoria actualizada = categoriaService.actualizar(id, categoria);

        return ResponseEntity.ok(
                new CategoriaResponseDTO(
                        actualizada.getId(),
                        actualizada.getNombre(),
                        actualizada.getActiva()
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
