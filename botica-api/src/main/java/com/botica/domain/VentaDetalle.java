package com.botica.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "venta_detalle")
@Getter @Setter
public class VentaDetalle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    // cantidad y precioVenta están en la unidad_venta del producto (ej. tabletas)
    private Integer cantidad;
    private BigDecimal precioVenta;

    // Foto del producto al momento de la venta (si luego se edita el producto, el historial no cambia)
    private Boolean requiereReceta = false;
}
