package com.botica.lotes;

import com.botica.domain.Lote;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

// FEFO (First-Expired, First-Out): el lote que vence primero se ofrece primero. Es la política
// estándar en farmacia para minimizar mermas por vencimiento.
@Component
public class AsignacionFefo implements EstrategiaAsignacionLote {
    @Override
    public List<Lote> ordenar(List<Lote> lotes) {
        return lotes.stream().sorted(Comparator.comparing(Lote::getFechaVencimiento)).toList();
    }
}
