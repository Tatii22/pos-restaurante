package com.pos.service;

import com.pos.entity.TipoGasto;
import com.pos.exception.BadRequestException;
import com.pos.repository.TipoGastoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class TipoGastoService {

    private final TipoGastoRepository tipoGastoRepository;

    public TipoGasto crear(String nombre) {

        String normalizado = nombre.trim();

        if (tipoGastoRepository.existsByNombreIgnoreCase(normalizado)) {
            throw new BadRequestException("El tipo de gasto ya existe");
        }

        TipoGasto tipo = TipoGasto.builder()
                .nombre(normalizado)
                .build();

        return tipoGastoRepository.save(tipo);
    }

    public List<TipoGasto> listar() {
        return tipoGastoRepository.findAll();
    }
}

