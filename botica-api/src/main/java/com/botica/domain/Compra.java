package com.botica.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compra")
@Getter @Setter
public class Compra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private LocalDateTime fechaCompra;
    private String tipoComprobante;
    private String numeroComprobante;
    private BigDecimal total;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL)
    private List<CompraDetalle> detalles = new ArrayList<>();
}
