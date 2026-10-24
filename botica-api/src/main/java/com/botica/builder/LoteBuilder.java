package com.botica.builder;

import com.botica.domain.Laboratorio;
import com.botica.domain.Lote;
import com.botica.domain.Producto;

import java.math.BigDecimal;
import java.time.LocalDate;

// Builder de Lote: un lote nuevo exige producto, laboratorio, número y vencimiento. El stock
// arranca en cero salvo que se indique (siempre en la unidad de venta del producto).
public final class LoteBuilder {
    private Producto producto;
    private Laboratorio laboratorio;
    private String numeroLote;
    private LocalDate fechaVencimiento;
    private BigDecimal stockActual = BigDecimal.ZERO;

    private LoteBuilder() {}

    public static LoteBuilder nuevo() { return new LoteBuilder(); }

    public LoteBuilder producto(Producto producto) { this.producto = producto; return this; }
    public LoteBuilder laboratorio(Laboratorio laboratorio) { this.laboratorio = laboratorio; return this; }
    public LoteBuilder numeroLote(String numeroLote) { this.numeroLote = numeroLote; return this; }
    public LoteBuilder fechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; return this; }
    public LoteBuilder stockActual(BigDecimal stockActual) { this.stockActual = stockActual; return this; }

    public Lote build() {
        if (producto == null || laboratorio == null || numeroLote == null || fechaVencimiento == null || stockActual == null)
            throw new IllegalStateException(
                    "Lote incompleto: faltan producto, laboratorio, numeroLote, fechaVencimiento o stockActual");
        Lote l = new Lote();
        l.setProducto(producto);
        l.setLaboratorio(laboratorio);
        l.setNumeroLote(numeroLote);
        l.setFechaVencimiento(fechaVencimiento);
        l.setStockActual(stockActual);
        return l;
    }
}
