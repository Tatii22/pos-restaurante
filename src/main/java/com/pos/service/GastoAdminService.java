package com.pos.service;

import com.pos.dto.gastoAdmin.GastoAdminCreateDTO;
import com.pos.entity.*;
import com.pos.exception.BadRequestException;
import com.pos.repository.GastoAdminRepository;
import com.pos.repository.TipoGastoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GastoAdminService {

    private final GastoAdminRepository gastoAdminRepository;
    private final TipoGastoRepository tipoGastoRepository;

    public List<GastoAdmin> listarPorFecha(LocalDate fecha) {
        return gastoAdminRepository.findByFecha(fecha);
    }

    public List<GastoAdmin> listarPorRango(LocalDate inicio, LocalDate fin) {
        return gastoAdminRepository.findByFechaBetween(inicio, fin);
}

    public GastoAdmin registrar(
            GastoAdminCreateDTO dto,
            Usuario usuario
    ) {

        // 🔐 Solo ADMIN
        if (!usuario.getRol().getNombre().equals("ADMIN")) {
            throw new BadRequestException("Solo ADMIN puede registrar gastos administrativos");
        }
        if (dto.fecha().isAfter(LocalDate.now())) {
            throw new BadRequestException("La fecha del gasto no puede ser futura");
        }
        if (dto.fecha().isBefore(LocalDate.now().minusYears(2))) {
            throw new BadRequestException("Fecha demasiado antigua");
        }



        // 🏷️ Tipo de gasto
        TipoGasto tipo = tipoGastoRepository.findById(dto.tipoGastoId())
                .orElseThrow(() -> new BadRequestException("Tipo de gasto no existe"));


        GastoAdmin gasto = GastoAdmin.builder()
                .fecha(
                        dto.fecha() != null
                                ? dto.fecha()
                                : LocalDate.now()
                )
                .descripcion(dto.descripcion())
                .monto(dto.monto())
                .tipo(tipo)
                .usuario(usuario)
                .build();

        return gastoAdminRepository.save(gasto);
    }

    public void eliminarPorId(Long id, Usuario usuario) {
        if (usuario == null || usuario.getRol() == null || !"ADMIN".equals(usuario.getRol().getNombre())) {
            throw new BadRequestException("Solo ADMIN puede eliminar gastos administrativos");
        }
        GastoAdmin gasto = gastoAdminRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Gasto administrativo no encontrado"));
        gastoAdminRepository.delete(gasto);
    }
}
