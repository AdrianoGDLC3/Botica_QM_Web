package com.botica.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venta")
@Getter @Setter
public class Venta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null = venta de mostrador
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empleado_id")
    private Usuario empleado;

    private LocalDateTime fechaVenta;
    private String tipoComprobante;
    private String numeroComprobante;
    private String metodoPago;
    private BigDecimal total;

    // Receta: obligatoria si algún producto de la venta tiene requiereReceta. Se guarda que el
    // cliente la presentó y, opcionalmente, su número y el médico que la emitió.
    private Boolean recetaPresentada = false;
    private String recetaNumero;
    private String recetaMedico;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL)
    private List<VentaDetalle> detalles = new ArrayList<>();
}
