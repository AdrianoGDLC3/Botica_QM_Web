package com.botica.comprobante;

import com.botica.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Cliente de la Abstract Factory (ver FabricaComprobante): Spring le inyecta TODAS las fábricas
// concretas (FabricaBoleta, FabricaFactura, ...) y aquí se indexan por tipo, una sola vez, al
// construirse. VentaService sigue pidiendo paraTipo(tipo) igual que antes y no sabe cuántas
// fábricas existen. Si dos fábricas declaran el mismo tipo, la app falla al arrancar (no en
// producción, a mitad de una venta).
//
// Es un @Component (Singleton gestionado por Spring); el estado real, el correlativo, vive en la
// base de datos porque varias instancias de la app deben compartirlo.
@Component
public class GeneradorComprobanteFactory {
    private final Map<String, FabricaComprobante> fabricas;

    public GeneradorComprobanteFactory(List<FabricaComprobante> fabricas) {
        this.fabricas = fabricas.stream()
                .collect(Collectors.toUnmodifiableMap(FabricaComprobante::tipo, f -> f));
    }

    public GeneradorComprobante paraTipo(String tipoComprobante) {
        FabricaComprobante fabrica = tipoComprobante == null ? null : fabricas.get(tipoComprobante);
        if (fabrica == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tipo de comprobante no soportado: " + tipoComprobante);
        return fabrica.crearGenerador();
    }
}
