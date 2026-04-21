package com.pos.service;

import com.pos.dto.gastoAdmin.GastoAdminCreateDTO;
import com.pos.entity.GastoAdmin;
import com.pos.entity.TipoGasto;
import com.pos.entity.Usuario;
import com.pos.exception.BadRequestException;
import com.pos.repository.GastoAdminRepository;
import com.pos.repository.TipoGastoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
        if (!usuario.getRol().getNombre().equals("ADMIN")) {
            throw new BadRequestException("Solo ADMIN puede registrar gastos administrativos");
        }
        if (dto.fecha().isAfter(LocalDate.now())) {
            throw new BadRequestException("La fecha del gasto no puede ser futura");
        }
        if (dto.fecha().isBefore(LocalDate.now().minusYears(2))) {
            throw new BadRequestException("Fecha demasiado antigua");
        }

        TipoGasto tipo = tipoGastoRepository.findById(dto.tipoGastoId())
                .orElseThrow(() -> new BadRequestException("Tipo de gasto no existe"));

        BigDecimal montoEfectivo = resolverMontoEfectivo(dto.monto(), dto.montoEfectivo(), dto.montoTransferencia());
        BigDecimal montoTransferencia = nonNegative(dto.montoTransferencia());
        BigDecimal montoTotal = montoEfectivo.add(montoTransferencia);
        validarMontoTotal(montoTotal);

        GastoAdmin gasto = GastoAdmin.builder()
                .fecha(dto.fecha() != null ? dto.fecha() : LocalDate.now())
                .descripcion(dto.descripcion())
                .monto(montoTotal)
                .montoEfectivo(montoEfectivo)
                .montoTransferencia(montoTransferencia)
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

    private BigDecimal resolverMontoEfectivo(BigDecimal montoLegacy, BigDecimal montoEfectivo, BigDecimal montoTransferencia) {
        BigDecimal efectivo = nonNegative(montoEfectivo);
        BigDecimal transferencia = nonNegative(montoTransferencia);
        if (efectivo.compareTo(BigDecimal.ZERO) == 0
                && transferencia.compareTo(BigDecimal.ZERO) == 0
                && montoLegacy != null) {
            return nonNegative(montoLegacy);
        }
        return efectivo;
    }

    private void validarMontoTotal(BigDecimal montoTotal) {
        if (montoTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Debes registrar un monto mayor a 0 en efectivo, transferencia o ambos");
        }
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }
}
