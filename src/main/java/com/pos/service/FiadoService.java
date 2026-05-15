package com.pos.service;

import com.pos.dto.fiado.AbonoFiadoCreateDTO;
import com.pos.dto.fiado.AbonoFiadoResponseDTO;
import com.pos.dto.fiado.DeudorCreateDTO;
import com.pos.dto.fiado.DeudorDetalleDTO;
import com.pos.dto.fiado.DeudorResponseDTO;
import com.pos.entity.AbonoFiado;
import com.pos.entity.CondicionPago;
import com.pos.entity.Deudor;
import com.pos.entity.EstadoTurno;
import com.pos.entity.EstadoVenta;
import com.pos.entity.FormaPago;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Usuario;
import com.pos.entity.Venta;
import com.pos.exception.BadRequestException;
import com.pos.repository.AbonoFiadoRepository;
import com.pos.repository.DeudorRepository;
import com.pos.repository.TurnoCajaRepository;
import com.pos.repository.VentaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FiadoService {

    private final DeudorRepository deudorRepository;
    private final VentaRepository ventaRepository;
    private final AbonoFiadoRepository abonoFiadoRepository;
    private final TurnoCajaRepository turnoCajaRepository;
    private final VentaPagoDetalleService ventaPagoDetalleService;

    public List<DeudorResponseDTO> listarDeudores(boolean soloConDeuda) {
        return deudorRepository.findAllByOrderByNombreAsc().stream()
                .map(this::toResumen)
                .filter(dto -> !soloConDeuda || dto.deudaTotal().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    public DeudorDetalleDTO obtenerDetalle(Long deudorId) {
        Deudor deudor = obtenerDeudor(deudorId);
        BigDecimal deudaTotal = calcularDeudaTotal(deudor);

        List<AbonoFiadoResponseDTO> abonos = new java.util.ArrayList<>();
        for (AbonoFiado a : abonoFiadoRepository.findByDeudorOrderByFechaDesc(deudor)) {
            abonos.add(new AbonoFiadoResponseDTO(
                a.getId(), a.getFecha(), a.getMonto(), a.getMontoEfectivo(), a.getMontoTransferencia(),
                a.getFormaPago(), a.getObservacion(),
                a.getUsuario() != null ? a.getUsuario().getUsername() : "-",
                a.getTurno() != null ? a.getTurno().getId() : null
            ));
        }

        List<com.pos.dto.venta.VentaResponseDTO> ventasPendientes = new java.util.ArrayList<>();
        for (Venta v : ventaRepository.findByDeudorAndEstadoAndSaldoPendienteGreaterThanOrderByFechaAsc(deudor, EstadoVenta.DESPACHADA, BigDecimal.ZERO)) {
            var pago = ventaPagoDetalleService.obtener(v.getId());
            ventasPendientes.add(new com.pos.dto.venta.VentaResponseDTO(
                v.getId(), v.getFecha(), v.getTipoVenta(), v.getEstado(), v.getClienteNombre(), v.getTelefono(),
                v.getDireccion(), v.getValorDomicilio(), v.getParaLlevar(), v.getDescuentoPorcentaje(),
                v.getDescuentoValor(), v.getTotal(), v.getFormaPago(),
                pago != null ? pago.pagoEfectivo() : BigDecimal.ZERO,
                pago != null ? pago.pagoTransferencia() : BigDecimal.ZERO,
                v.getCondicionPago(), v.getSaldoPendiente(),
                v.getDeudor() != null ? v.getDeudor().getId() : null
            ));
        }

        return new DeudorDetalleDTO(
            deudor.getId(),
            deudor.getNombre(),
            deudor.getTelefono(),
            deudaTotal,
            ventasPendientes,
            abonos
        );
    }

    @Transactional
    public DeudorResponseDTO crearDeudor(DeudorCreateDTO dto) {
        String telefono = normalizarTelefono(dto.telefono());
        if (deudorRepository.findByTelefono(telefono).isPresent()) {
            throw new BadRequestException("Ya existe un deudor con ese telefono");
        }
        LocalDateTime ahora = LocalDateTime.now();
        Deudor deudor = deudorRepository.save(
                Deudor.builder()
                        .nombre(normalizarNombre(dto.nombre()))
                        .telefono(telefono)
                        .activo(true)
                        .fechaCreacion(ahora)
                        .fechaActualizacion(ahora)
                        .build()
        );
        return toResumen(deudor);
    }

    @Transactional
    public AbonoFiadoResponseDTO registrarAbono(AbonoFiadoCreateDTO dto, Usuario usuario) {
        Deudor deudor = obtenerDeudor(dto.deudorId());
        BigDecimal montoEfectivo = nonNegative(dto.montoEfectivo());
        BigDecimal montoTransferencia = nonNegative(dto.montoTransferencia());
        BigDecimal monto = montoEfectivo.add(montoTransferencia);
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Debes registrar un abono mayor a 0");
        }

        BigDecimal deudaActual = calcularDeudaTotal(deudor);
        if (deudaActual.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("El deudor no tiene saldo pendiente");
        }

        TurnoCaja turno = turnoCajaRepository.findByEstadoIn(List.of(EstadoTurno.ABIERTO, EstadoTurno.SIMULADO))
                .orElseThrow(() -> new BadRequestException("No hay turno activo para registrar el abono"));

        BigDecimal restante = monto;
        List<Venta> ventasPendientes = ventaRepository.findByDeudorAndEstadoAndSaldoPendienteGreaterThanOrderByFechaAsc(
                deudor,
                EstadoVenta.DESPACHADA,
                BigDecimal.ZERO
        );
        for (Venta venta : ventasPendientes) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal aplicado = venta.getSaldoPendiente().min(restante);
            venta.setSaldoPendiente(venta.getSaldoPendiente().subtract(aplicado));
            ventaRepository.save(venta);
            restante = restante.subtract(aplicado);
        }

        BigDecimal aplicadoTotal = monto.subtract(restante);
        if (aplicadoTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("No fue posible aplicar el abono");
        }

        FormaPago formaPago = resolverFormaPago(montoEfectivo, montoTransferencia);
        AbonoFiado abono = abonoFiadoRepository.save(
                AbonoFiado.builder()
                        .fecha(LocalDateTime.now())
                        .monto(aplicadoTotal)
                        .montoEfectivo(montoEfectivo.min(aplicadoTotal))
                        .montoTransferencia(aplicadoTotal.subtract(montoEfectivo.min(aplicadoTotal)))
                        .formaPago(formaPago)
                        .observacion(normalizarObservacion(dto.observacion()))
                        .deudor(deudor)
                        .usuario(usuario)
                        .turno(turno)
                        .build()
        );

        turno.setTotalVentas(turno.getTotalVentas().add(aplicadoTotal));
        turnoCajaRepository.save(turno);

        return toAbonoDto(abono);
    }

    @Transactional
    public Deudor resolverDeudorVenta(Boolean fiado, Long deudorId, String deudorNombre, String deudorTelefono) {
        if (!Boolean.TRUE.equals(fiado)) {
            return null;
        }
        if (deudorId != null) {
            return obtenerDeudor(deudorId);
        }
        String telefono = normalizarTelefono(deudorTelefono);
        String nombre = normalizarNombre(deudorNombre);
        return deudorRepository.findByTelefono(telefono)
                .map(existente -> {
                    existente.setNombre(nombre);
                    existente.setActivo(true);
                    existente.setFechaActualizacion(LocalDateTime.now());
                    return deudorRepository.save(existente);
                })
                .orElseGet(() -> deudorRepository.save(
                        Deudor.builder()
                                .nombre(nombre)
                                .telefono(telefono)
                                .activo(true)
                                .fechaCreacion(LocalDateTime.now())
                                .fechaActualizacion(LocalDateTime.now())
                                .build()
                ));
    }

    public BigDecimal calcularDeudaTotal(Deudor deudor) {
        return ventaRepository.sumarSaldoPendientePorDeudor(deudor, CondicionPago.FIADO, EstadoVenta.DESPACHADA);
    }

    public Deudor obtenerDeudor(Long deudorId) {
        return deudorRepository.findById(deudorId)
                .orElseThrow(() -> new BadRequestException("Deudor no encontrado"));
    }

    private DeudorResponseDTO toResumen(Deudor deudor) {
        List<Venta> pendientes = ventaRepository.findByDeudorAndEstadoAndSaldoPendienteGreaterThanOrderByFechaAsc(
                deudor,
                EstadoVenta.DESPACHADA,
                BigDecimal.ZERO
        );
        BigDecimal deuda = pendientes.stream()
                .map(Venta::getSaldoPendiente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DeudorResponseDTO(
                deudor.getId(),
                deudor.getNombre(),
                deudor.getTelefono(),
                deudor.getActivo(),
                deuda,
                pendientes.size()
        );
    }

    private AbonoFiadoResponseDTO toAbonoDto(AbonoFiado abono) {
        return new AbonoFiadoResponseDTO(
                abono.getId(),
                abono.getFecha(),
                abono.getMonto(),
                abono.getMontoEfectivo(),
                abono.getMontoTransferencia(),
                abono.getFormaPago(),
                abono.getObservacion(),
                abono.getUsuario() != null ? abono.getUsuario().getUsername() : "-",
                abono.getTurno() != null ? abono.getTurno().getId() : null
        );
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    private FormaPago resolverFormaPago(BigDecimal montoEfectivo, BigDecimal montoTransferencia) {
        if (montoTransferencia.compareTo(BigDecimal.ZERO) > 0 && montoEfectivo.compareTo(BigDecimal.ZERO) <= 0) {
            return FormaPago.TRANSFERENCIA;
        }
        return FormaPago.EFECTIVO;
    }

    private String normalizarNombre(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("El nombre del deudor es obligatorio");
        }
        String clean = value.trim().replaceAll("\\s+", " ");
        return clean.length() > 100 ? clean.substring(0, 100) : clean;
    }

    private String normalizarTelefono(String value) {
        String clean = value == null ? "" : value.replaceAll("\\D", "");
        if (clean.length() < 7 || clean.length() > 15) {
            throw new BadRequestException("El telefono del deudor debe tener entre 7 y 15 digitos");
        }
        return clean;
    }

    private String normalizarObservacion(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String clean = value.trim().replace("\r", " ").replace("\n", " ");
        return clean.length() > 255 ? clean.substring(0, 255) : clean;
    }

    private com.pos.dto.venta.VentaResponseDTO toVentaDto(Venta venta) {
        var pago = venta.getId() != null ? ventaPagoDetalleService.obtener(venta.getId()) : null;
        return new com.pos.dto.venta.VentaResponseDTO(
                venta.getId(),
                venta.getFecha(),
                venta.getTipoVenta(),
                venta.getEstado(),
                venta.getClienteNombre(),
                venta.getTelefono(),
                venta.getDireccion(),
                venta.getValorDomicilio(),
                venta.getParaLlevar(),
                venta.getDescuentoPorcentaje(),
                venta.getDescuentoValor(),
                venta.getTotal(),
                venta.getFormaPago(),
                pago != null ? pago.pagoEfectivo() : BigDecimal.ZERO,
                pago != null ? pago.pagoTransferencia() : BigDecimal.ZERO,
                venta.getCondicionPago(),
                venta.getSaldoPendiente(),
                venta.getDeudor() != null ? venta.getDeudor().getId() : null
        );
    }
}
