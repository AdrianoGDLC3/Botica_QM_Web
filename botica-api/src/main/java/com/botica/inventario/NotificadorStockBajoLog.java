package com.botica.inventario;

import com.botica.domain.Lote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Observador concreto: por ahora solo deja constancia en el log. Al ser un @Component, Spring lo
// inyecta automáticamente en la lista de observadores de VentaService (ver List<ObservadorInventario>).
@Slf4j
@Component
public class NotificadorStockBajoLog implements ObservadorInventario {
    @Override
    public void onStockBajo(Lote lote) {
        log.warn("Stock bajo: {} (lote {}) quedó en {} {}", lote.getProducto().getNombre(), lote.getNumeroLote(),
                lote.getStockActual(), lote.getProducto().getUnidadVenta().toLowerCase());
    }
}
