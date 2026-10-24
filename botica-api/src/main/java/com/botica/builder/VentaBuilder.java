package com.botica.builder;

import com.botica.domain.Usuario;
import com.botica.domain.Venta;
import com.botica.domain.VentaDetalle;
import com.botica.util.Textos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Builder de Venta: arma la cabecera paso a paso (con los datos opcionales normalizados: vacío
// -> null) y las líneas de detalle, y recién en build() crea la entidad ya consistente: el
// empleado y la fecha son obligatorios, y cada detalle queda enlazado a su venta.
// Cada build() produce una Venta nueva; el builder no comparte instancias entre llamadas.
public final class VentaBuilder {
    private Usuario cliente;                 // null = venta de mostrador
    private Usuario empleado;
    private LocalDateTime fechaVenta;
    private String tipoComprobante;
    private String numeroComprobante;
    private String metodoPago;
    private boolean recetaPresentada;
    private String recetaNumero;
    private String recetaMedico;
    private BigDecimal total;
    private final List<VentaDetalle> detalles = new ArrayList<>();

    private VentaBuilder() {}

    public static VentaBuilder nueva() { return new VentaBuilder(); }

    public VentaBuilder cliente(Usuario cliente) { this.cliente = cliente; return this; }
    public VentaBuilder empleado(Usuario empleado) { this.empleado = empleado; return this; }
    public VentaBuilder fechaVenta(LocalDateTime fechaVenta) { this.fechaVenta = fechaVenta; return this; }
    public VentaBuilder tipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; return this; }
    public VentaBuilder numeroComprobante(String numeroComprobante) { this.numeroComprobante = numeroComprobante; return this; }
    public VentaBuilder metodoPago(String metodoPago) { this.metodoPago = Textos.opcional(metodoPago); return this; }

    // presentada: null se toma como false. Número y médico son opcionales.
    public VentaBuilder receta(Boolean presentada, String numero, String medico) {
        this.recetaPresentada = Boolean.TRUE.equals(presentada);
        this.recetaNumero = Textos.opcional(numero);
        this.recetaMedico = Textos.opcional(medico);
        return this;
    }

    public VentaBuilder total(BigDecimal total) { this.total = total; return this; }
    public VentaBuilder agregarDetalle(VentaDetalle detalle) { this.detalles.add(detalle); return this; }

    public Venta build() {
        if (empleado == null || fechaVenta == null || tipoComprobante == null)
            throw new IllegalStateException("Venta incompleta: faltan empleado, fecha o tipo de comprobante");
        Venta v = new Venta();
        v.setCliente(cliente);
        v.setEmpleado(empleado);
        v.setFechaVenta(fechaVenta);
        v.setTipoComprobante(tipoComprobante);
        v.setNumeroComprobante(numeroComprobante);
        v.setMetodoPago(metodoPago);
        v.setRecetaPresentada(recetaPresentada);
        v.setRecetaNumero(recetaNumero);
        v.setRecetaMedico(recetaMedico);
        v.setTotal(total);
        for (VentaDetalle d : detalles) {
            d.setVenta(v);
            v.getDetalles().add(d);
        }
        return v;
    }
}
