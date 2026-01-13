package com.pos.service;

import com.pos.entity.Producto;
import com.pos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.pos.exception.ResourceNotFoundException;
import com.pos.exception.BadRequestException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    public Producto crear(Producto producto) {
        if (producto == null) {
            throw new BadRequestException("Producto no puede ser nulo");
        }
        return productoRepository.save(producto);
    }

    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    public Producto obtenerPorId(Long id) {
        if (id == null) {
            throw new BadRequestException("ID no puede ser nulo");
        }
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    }


    public Producto actualizar(Long id, Producto producto) {
        Producto existente = obtenerPorId(id);
        existente.setNombre(producto.getNombre());
        existente.setPrecio(producto.getPrecio());
        existente.setActivo(producto.getActivo());
        existente.setCategoria(producto.getCategoria());
        existente.setTipoVenta(producto.getTipoVenta());
        return productoRepository.save(existente);
    }

    public void eliminar(Long id) {
        if (id == null) {
            throw new BadRequestException("ID no puede ser nulo");
        }
        productoRepository.deleteById(id);
    }
    
}
