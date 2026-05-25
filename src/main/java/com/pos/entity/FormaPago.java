package com.pos.entity;

public enum FormaPago {
    EFECTIVO,
    TRANSFERENCIA,
    /** Venta entregada sin cobro inmediato; el cliente pagará posteriormente mediante abonos. */
    FIADO
}
