package com.botica.comprobante;

import lombok.extern.slf4j.Slf4j;

// Decorator: agrega una traza de auditoría a cualquier GeneradorComprobante sin tocar su lógica
// de numeración. Envuelve la implementación real (GeneradorComprobanteSerie) o cualquier otra
// que se agregue después (por ejemplo, una que consulte un sistema externo de facturación),
// sin que VentaService se entere del cambio.
@Slf4j
public class GeneradorComprobanteConLogging implements GeneradorComprobante {
    private final GeneradorComprobante delegado;

    public GeneradorComprobanteConLogging(GeneradorComprobante delegado) {
        this.delegado = delegado;
    }

    @Override
    public String siguienteNumero() {
        String numero = delegado.siguienteNumero();
        log.info("Comprobante generado: {}", numero);
        return numero;
    }
}
