package com.pos.service;

import com.pos.entity.Categoria;
import com.pos.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.pos.exception.ResourceNotFoundException;
import com.pos.exception.BadRequestException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public Categoria crear(Categoria categoria) {
        if (categoria == null) {
            throw new BadRequestException("Categoría no puede ser nula");
        }
        return categoriaRepository.save(categoria);
    }

    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }

    public Categoria obtenerPorId(Long id) {
        if (id == null) {
            throw new BadRequestException("ID no puede ser nulo");
        }
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
    }

    public Categoria actualizar(Long id, Categoria categoria) {
        Categoria existente = obtenerPorId(id);
        existente.setNombre(categoria.getNombre());
        existente.setActiva(categoria.getActiva());
        return categoriaRepository.save(existente);
    }

    public void eliminar(Long id) {
        if (id == null) {
            throw new BadRequestException("ID no puede ser nulo");
        }
        categoriaRepository.deleteById(id);
    }
}
