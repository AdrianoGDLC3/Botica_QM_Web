package com.botica.builder;

import com.botica.domain.CompraDetalle;
import com.botica.domain.Lote;

import java.math.BigDecimal;

// Builder de una línea de compra. cantidad = cajas; unidadesPorCaja = unidades que trae cada
// una en ESTA compra; precioCompra = costo por caja. La Compra la enlaza CompraBuilder.
public final class CompraDetalleBuilder {
    private Lote lote;
    private Integer cantidad;
    private Integer unidadesPorCaja;
    private BigDecimal precioCompra;

    private CompraDetalleBuilder() {}

    public static CompraDetalleBuilder nuevo() { return new CompraDetalleBuilder(); }

    public CompraDetalleBuilder lote(Lote lote) { this.lote = lote; return this; }
    public CompraDetalleBuilder cantidad(Integer cantidad) { this.cantidad = cantidad; return this; }
    public CompraDetalleBuilder unidadesPorCaja(Integer unidadesPorCaja) { this.unidadesPorCaja = unidadesPorCaja; return this; }
    public CompraDetalleBuilder precioCompra(BigDecimal precioCompra) { this.precioCompra = precioCompra; return this; }

    public CompraDetalle build() {
        if (lote == null || cantidad == null || unidadesPorCaja == null || precioCompra == null)
            throw new IllegalStateException(
                    "CompraDetalle incompleto: faltan lote, cantidad, unidadesPorCaja o precioCompra");
        CompraDetalle d = new CompraDetalle();
        d.setLote(lote);
        d.setCantidad(cantidad);
        d.setUnidadesPorCaja(unidadesPorCaja);
        d.setPrecioCompra(precioCompra);
        return d;
    }
}
