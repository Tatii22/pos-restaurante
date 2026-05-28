package com.pos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "turnos_caja")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    /** Número secuencial del turno dentro del mes (se reinicia cada mes) */
    @Column(name = "numero_turno", nullable = false)
    private Integer numeroTurno;

    @Column(nullable = false)
    private LocalDateTime fechaApertura;

    private LocalDateTime fechaCierre;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoInicial;

    @Column(precision = 12, scale = 2)
    private BigDecimal montoFinal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalVentas;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalGastos;

    // ── Conciliación de caja persistida al momento del cierre real ──────────

    /** Saldo físico de efectivo esperado = montoInicial + recaudoEfectivo − gastosEfectivo */
    @Column(precision = 12, scale = 2)
    private BigDecimal esperado;

    /**
     * Solo el monto en que la caja quedó CORTA (diferencia negativa en efectivo).
     * Si la caja tiene sobrante, este campo es CERO.
     * Usar diferenciaEfectivo para el valor con signo completo.
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal faltante;

    /** Efectivo físico contado por el cajero al cierre. */
    @Column(precision = 12, scale = 2)
    private BigDecimal efectivoContado;

    /** Transferencias revisadas en la app bancaria al cierre. */
    @Column(precision = 12, scale = 2)
    private BigDecimal transferenciasVerificadas;

    /**
     * Diferencia de efectivo con signo: efectivoContado − esperado.
     * Negativo = falta dinero. Positivo = sobra dinero.
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal diferenciaEfectivo;

    /**
     * Diferencia de transferencias con signo: transferenciasVerificadas − transferenciasNetas.
     * Negativo = falta. Positivo = sobra.
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal diferenciaTransferencias;

    /** efectivoContado + transferenciasVerificadas */
    @Column(precision = 12, scale = 2)
    private BigDecimal totalVerificado;

    /**
     * Diferencia total con signo: totalVerificado − totalOperativoTurno.
     * Este es el número que define si la caja "cuadra".
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal diferenciaTotal;

    /**
     * Justificación obligatoria cuando |diferenciaTotal| supera el umbral configurado.
     * Permite al gerente entender por qué cerró descuadrada.
     */
    @Column(length = 500)
    private String observacionCierre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTurno estado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // ── Campos calculados en memoria (no persistidos) ────────────────────────

    /** transferenciasNetas = recaudoTransferencia − gastosTransferencia. Solo en memoria. */
    @Transient
    private BigDecimal transferenciasNetas;

    /** Base + recaudo − gastos (efectivo + transferencias). Solo en memoria. */
    @Transient
    private BigDecimal totalOperativoTurno;

    /** Gastos administrativos del período del turno (por fecha). Solo en memoria. */
    @Transient
    private BigDecimal totalGastosAdmin;

    /** Todo el dinero recibido sin descontar gastos. Solo en memoria. */
    @Transient
    private BigDecimal recaudoBruto;

    /** recaudoBruto − gastosCaja − gastosAdmin. Solo en memoria. */
    @Transient
    private BigDecimal gananciaNeta;

    /** Umbral de descuadre configurado en el servidor. Solo en memoria. */
    @Transient
    private BigDecimal umbralDescuadre;
}
