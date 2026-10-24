package com.botica.inventario;

import com.botica.domain.Lote;

// Observer: cualquiera que necesite reaccionar cuando el stock de un lote queda bajo tras una
// venta. VentaService (el "sujeto") avisa a todos los que implementen esta interfaz sin conocerlos;
// hoy solo hay un observador que registra un log, pero se puede agregar uno que mande un correo o
// un WhatsApp implementando esta interfaz, sin tocar VentaService.
public interface ObservadorInventario {
    void onStockBajo(Lote lote);
}
