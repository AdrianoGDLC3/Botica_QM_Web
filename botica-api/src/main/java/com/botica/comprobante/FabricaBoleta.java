package com.botica.comprobante;

import com.botica.repo.SerieComprobanteRepository;
import org.springframework.stereotype.Component;

// Fábrica concreta: familia de la BOLETA (serie B001)
@Component
public class FabricaBoleta extends FabricaComprobanteSerie {
    public FabricaBoleta(SerieComprobanteRepository repo) {
        super(repo, "BOLETA", "B001");
    }
}
