package com.botica.validacion;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Singleton: Spring reúne automáticamente todos los ValidadorAtributos (uno por categoría, más
// SUPLEMENTO reutilizando el de MEDICAMENTO) y este registro los indexa por nombre de categoría,
// una sola vez al arrancar. CatalogoService le pregunta a este único punto, sin conocer cuántos
// validadores existen ni cuáles. Agregar una categoría con reglas propias es agregar OTRO
// ValidadorAtributos (@Component); este registro no cambia.
@Component
@RequiredArgsConstructor
public class RegistroValidadoresAtributos {
    private final List<ValidadorAtributos> validadores;
    private Map<String, ValidadorAtributos> porCategoria;

    @PostConstruct
    void construir() {
        porCategoria = validadores.stream().collect(Collectors.toMap(ValidadorAtributos::categoria, v -> v));
        porCategoria.putIfAbsent("SUPLEMENTO", porCategoria.get("MEDICAMENTO")); // mismos campos
    }

    public void validar(String categoria, Map<String, Object> atributos) {
        ValidadorAtributos v = porCategoria.get(categoria);
        if (v != null) v.validar(atributos);
    }
}
