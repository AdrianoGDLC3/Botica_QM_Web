package com.botica.lotes;

import com.botica.domain.Lote;

import java.util.List;

// Strategy: qué lote conviene despachar primero cuando hay varios con stock disponible. Hoy solo
// existe FEFO (el que vence primero, primero), pero mañana podría haber otra política (por ejemplo,
// agotar antes el lote comprado más antiguo) sin tocar a quien la usa (CatalogoService).
public interface EstrategiaAsignacionLote {
    List<Lote> ordenar(List<Lote> lotes);
}
