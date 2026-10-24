package com.botica.builder;

import com.botica.domain.Compra;
import com.botica.domain.CompraDetalle;
import com.botica.domain.Proveedor;
import com.botica.domain.Usuario;
import com.botica.util.Textos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Builder de Compra: mismo esquema que VentaBuilder. Tipo y número de comprobante son opcionales
// (vacío -> null); proveedor, usuario y fecha son obligatorios; cada detalle queda enlazado a su
// compra en build().
public final class CompraBuilder {
    private Proveedor proveedor;
    private Usuario usuario;
    private LocalDateTime fechaCompra;
    private String tipoComprobante;
    private String numeroComprobante;
    private BigDecimal total;
    private final List<CompraDetalle> detalles = new ArrayList<>();

    private CompraBuilder() {}

    public static CompraBuilder nueva() { return new CompraBuilder(); }

    public CompraBuilder proveedor(Proveedor proveedor) { this.proveedor = proveedor; return this; }
    public CompraBuilder usuario(Usuario usuario) { this.usuario = usuario; return this; }
    public CompraBuilder fechaCompra(LocalDateTime fechaCompra) { this.fechaCompra = fechaCompra; return this; }
    public CompraBuilder tipoComprobante(String tipoComprobante) { this.tipoComprobante = Textos.opcional(tipoComprobante); return this; }
    public CompraBuilder numeroComprobante(String numeroComprobante) { this.numeroComprobante = Textos.opcional(numeroComprobante); return this; }
    public CompraBuilder total(BigDecimal total) { this.total = total; return this; }
    public CompraBuilder agregarDetalle(CompraDetalle detalle) { this.detalles.add(detalle); return this; }

    public Compra build() {
        if (proveedor == null || usuario == null || fechaCompra == null)
            throw new IllegalStateException("Compra incompleta: faltan proveedor, usuario o fecha");
        Compra c = new Compra();
        c.setProveedor(proveedor);
        c.setUsuario(usuario);
        c.setFechaCompra(fechaCompra);
        c.setTipoComprobante(tipoComprobante);
        c.setNumeroComprobante(numeroComprobante);
        c.setTotal(total);
        for (CompraDetalle d : detalles) {
            d.setCompra(c);
            c.getDetalles().add(d);
        }
        return c;
    }
}
