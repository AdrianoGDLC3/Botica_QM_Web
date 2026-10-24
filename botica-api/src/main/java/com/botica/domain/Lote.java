package com.botica.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

// stockActual siempre está en la unidad_venta del producto (tabletas, unidades, frascos...).
// Al comprar entra cajas × unidades por caja (compra_detalle); al vender sale por unidad.
@Entity
@Table(name = "lote")
@Getter @Setter
public class Lote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "laboratorio_id")
    private Laboratorio laboratorio;

    private String numeroLote;
    private LocalDate fechaVencimiento;
    private BigDecimal stockActual = BigDecimal.ZERO;
}
