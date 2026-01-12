package com.pos.controller;

import com.pos.dto.producto.*;
import com.pos.entity.Categoria;
import com.pos.entity.Producto;
import com.pos.service.CategoriaService;
import com.pos.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/productos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProductoController {


    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    @PostMapping
    public ResponseEntity<ProductoResponseDTO> crear(
            @Valid @RequestBody ProductoCreateDTO dto) {

        Categoria categoria = categoriaService.obtenerPorId(dto.getCategoriaId());

        Producto producto = new Producto();
        producto.setNombre(dto.getNombre());
        producto.setPrecio(dto.getPrecio());
        producto.setActivo(dto.getActivo());
        producto.setCategoria(categoria);

        Producto guardado = productoService.crear(producto);

        return ResponseEntity.ok(
                new ProductoResponseDTO(
                        guardado.getId(),
                        guardado.getNombre(),
                        guardado.getPrecio(),
                        guardado.getActivo(),
                        categoria.getId(),
                        categoria.getNombre()
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<ProductoResponseDTO>> listar() {

        List<ProductoResponseDTO> lista =
                productoService.listar()
                        .stream()
                        .map(p -> new ProductoResponseDTO(
                                p.getId(),
                                p.getNombre(),
                                p.getPrecio(),
                                p.getActivo(),
                                p.getCategoria().getId(),
                                p.getCategoria().getNombre()
                        ))
                        .toList();

        return ResponseEntity.ok(lista);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoUpdateDTO dto) {

        Categoria categoria = categoriaService.obtenerPorId(dto.getCategoriaId());

        Producto producto = new Producto();
        producto.setNombre(dto.getNombre());
        producto.setPrecio(dto.getPrecio());
        producto.setActivo(dto.getActivo());
        producto.setCategoria(categoria);

        Producto actualizado = productoService.actualizar(id, producto);

        return ResponseEntity.ok(
                new ProductoResponseDTO(
                        actualizado.getId(),
                        actualizado.getNombre(),
                        actualizado.getPrecio(),
                        actualizado.getActivo(),
                        categoria.getId(),
                        categoria.getNombre()
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
