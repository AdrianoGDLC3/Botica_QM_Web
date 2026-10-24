package com.botica.comprobante;

// Algo que produce el siguiente número de comprobante de una serie (B001-000123...).
public interface GeneradorComprobante {
    String siguienteNumero();
}
