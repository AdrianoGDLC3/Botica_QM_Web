package com.botica.comprobante;

// Abstract Factory: una fábrica por TIPO de comprobante (boleta, factura...) que crea la familia
// completa de productos que deben ser coherentes entre sí:
//   - DescriptorComprobante: de qué tipo y serie es
//   - GeneradorComprobante:  cómo se numera, ya con su Decorator de logging
// Como ambos salen de la misma fábrica, es imposible armar, por ejemplo, un generador de la
// serie F001 con el descriptor de una BOLETA. Agregar un tipo nuevo (NOTA_CREDITO...) es agregar
// OTRA implementación @Component de esta interfaz; ni GeneradorComprobanteFactory ni VentaService
// cambian.
public interface FabricaComprobante {
    String tipo();                               // clave con la que se elige la fábrica (BOLETA, FACTURA)
    DescriptorComprobante crearDescriptor();
    GeneradorComprobante crearGenerador();
}
