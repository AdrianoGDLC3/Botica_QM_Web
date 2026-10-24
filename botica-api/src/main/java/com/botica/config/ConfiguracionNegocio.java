package com.botica.config;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Set;

// Singleton: parámetros de negocio que son iguales para toda la app y que no cambian mientras
// corre (zona horaria de la botica, umbral de stock bajo, categorías de producto válidas).
// Antes estaban repartidos como constantes sueltas en Tiempo, VentaService y CatalogoService.
//
// Implementación "initialization-on-demand holder":
//  - constructor privado: nadie más puede crear instancias;
//  - la instancia vive en una clase interna que la JVM carga (y construye) recién en el primer
//    uso de instancia(), y lo hace de forma thread-safe sin necesidad de synchronized;
//  - el estado es inmutable (campos final, Set.of), así que compartirla entre hilos es seguro;
//  - es final y no implementa Serializable ni Cloneable, por lo que no se puede duplicar
//    por herencia, deserialización ni clone().
//
// No es un bean de Spring a propósito: se usa también desde utilidades estáticas (Tiempo).
// Los @Service/@Component del proyecto siguen siendo singletons, pero gestionados por Spring.
public final class ConfiguracionNegocio {

    private final ZoneId zonaHoraria;
    private final BigDecimal umbralStockBajo;
    private final Set<String> categoriasValidas;

    private ConfiguracionNegocio() {
        this.zonaHoraria = ZoneId.of("America/Lima");
        // Bajo este stock (en unidad de venta) se avisa a los observadores tras una venta.
        // Un umbral fijo es una simplificación: lo ideal sería configurable por producto.
        this.umbralStockBajo = BigDecimal.TEN;
        this.categoriasValidas = Set.of(
                "MEDICAMENTO", "SUPLEMENTO", "PANALES", "BIBERON", "HIGIENE",
                "CUIDADO_PERSONAL", "CURACION", "DISPOSITIVO_MEDICO", "ACCESORIO", "OTRO");
    }

    private static final class Holder {
        private static final ConfiguracionNegocio INSTANCIA = new ConfiguracionNegocio();
    }

    public static ConfiguracionNegocio instancia() {
        return Holder.INSTANCIA;
    }

    public ZoneId zonaHoraria() { return zonaHoraria; }

    public BigDecimal umbralStockBajo() { return umbralStockBajo; }

    public boolean esCategoriaValida(String categoria) { return categoriasValidas.contains(categoria); }
}
