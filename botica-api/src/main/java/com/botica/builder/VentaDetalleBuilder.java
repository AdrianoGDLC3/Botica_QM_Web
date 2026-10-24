package com.botica.builder;

import com.botica.domain.Lote;
import com.botica.domain.VentaDetalle;

import java.math.BigDecimal;

// Builder de una línea de venta. La referencia a la Venta NO se pasa aquí: la fija VentaBuilder
// al agregar el detalle, para que la relación bidireccional nunca quede a medias.
public final class VentaDetalleBuilder {
    private Lote lote;
    private Integer cantidad;
    private BigDecimal precioVenta;
    private boolean requiereReceta;

    private VentaDetalleBuilder() {}

    public static VentaDetalleBuilder nuevo() { return new VentaDetalleBuilder(); }

    public VentaDetalleBuilder lote(Lote lote) { this.lote = lote; return this; }
    public VentaDetalleBuilder cantidad(Integer cantidad) { this.cantidad = cantidad; return this; }
    public VentaDetalleBuilder precioVenta(BigDecimal precioVenta) { this.precioVenta = precioVenta; return this; }
    // Foto del producto al momento de vender (si luego se edita, el historial no cambia)
    public VentaDetalleBuilder requiereReceta(Boolean requiereReceta) {
        this.requiereReceta = Boolean.TRUE.equals(requiereReceta);
        return this;
    }

    public VentaDetalle build() {
        if (lote == null || cantidad == null || precioVenta == null)
            throw new IllegalStateException("VentaDetalle incompleto: faltan lote, cantidad o precioVenta");
        VentaDetalle d = new VentaDetalle();
        d.setLote(lote);
        d.setCantidad(cantidad);
        d.setPrecioVenta(precioVenta);
        d.setRequiereReceta(requiereReceta);
        return d;
    }
}
