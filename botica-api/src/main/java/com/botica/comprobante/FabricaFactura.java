package com.botica.comprobante;

import com.botica.repo.SerieComprobanteRepository;
import org.springframework.stereotype.Component;

// Fábrica concreta: familia de la FACTURA (serie F001)
@Component
public class FabricaFactura extends FabricaComprobanteSerie {
    public FabricaFactura(SerieComprobanteRepository repo) {
        super(repo, "FACTURA", "F001");
    }
}
