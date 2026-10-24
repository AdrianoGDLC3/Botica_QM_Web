package com.botica.comprobante;

// Producto B de la familia "comprobante": la identidad del tipo (BOLETA -> serie B001).
// Es inmutable; cada fábrica concreta produce el descriptor que va con SU generador.
public record DescriptorComprobante(String tipo, String serie) {}
