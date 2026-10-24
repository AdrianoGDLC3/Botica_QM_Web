package com.botica.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "compra_detalle")
@Getter @Setter
public class CompraDetalle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compra_id")
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    // cantidad = cajas (o paquetes) compradas; unidadesPorCaja = unidades que trae cada una.
    // El stock del lote sube en cantidad × unidadesPorCaja.
    private Integer cantidad;
    private Integer unidadesPorCaja;
    private BigDecimal precioCompra; // costo por caja
}
