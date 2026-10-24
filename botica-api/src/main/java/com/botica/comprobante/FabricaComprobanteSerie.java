package com.botica.comprobante;

import com.botica.repo.SerieComprobanteRepository;

// Base común de las fábricas concretas: todas numeran con un correlativo por serie guardado en
// la base (serie_comprobante), así que solo difieren en el tipo y la serie que reciben.
public abstract class FabricaComprobanteSerie implements FabricaComprobante {
    private final SerieComprobanteRepository repo;
    private final String tipo;
    private final String serie;

    protected FabricaComprobanteSerie(SerieComprobanteRepository repo, String tipo, String serie) {
        this.repo = repo;
        this.tipo = tipo;
        this.serie = serie;
    }

    @Override public String tipo() { return tipo; }

    @Override public DescriptorComprobante crearDescriptor() { return new DescriptorComprobante(tipo, serie); }

    @Override
    public GeneradorComprobante crearGenerador() {
        return new GeneradorComprobanteConLogging(new GeneradorComprobanteSerie(repo, crearDescriptor().serie()));
    }
}
