package com.pos.service;




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


