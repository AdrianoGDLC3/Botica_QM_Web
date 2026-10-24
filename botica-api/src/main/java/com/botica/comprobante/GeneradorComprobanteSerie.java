package com.botica.comprobante;

import com.botica.repo.SerieComprobanteRepository;

// Genera correlativos por serie, persistidos en la tabla serie_comprobante para que sean
// consecutivos de verdad (sin huecos entre boletas y facturas, y a salvo de carreras entre
// ventas simultáneas o varias instancias de la app). No es un bean de Spring: la Factory lo
// crea al vuelo con la serie que corresponda.
public class GeneradorComprobanteSerie implements GeneradorComprobante {
    private final SerieComprobanteRepository repo;
    private final String serie;

    public GeneradorComprobanteSerie(SerieComprobanteRepository repo, String serie) {
        this.repo = repo;
        this.serie = serie;
    }

    @Override
    public String siguienteNumero() {
        Long correlativo = repo.incrementarYObtener(serie);
        return serie + "-" + String.format("%06d", correlativo);
    }
}
