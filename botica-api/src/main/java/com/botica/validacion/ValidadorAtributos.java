package com.botica.validacion;

import com.botica.error.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Set;

// Strategy: cada categoría de producto sabe validar sus propios "atributos" (Producto.atributos,
// JSON libre). Antes el backend aceptaba cualquier forma sin revisarla; con esto se detectan
// valores fuera de catálogo antes de guardar, en espejo de los <select> del frontend. No exige
// que los campos estén presentes (siguen siendo opcionales): solo que, si vienen, tengan un
// valor válido.
public interface ValidadorAtributos {
    String categoria();
    void validar(Map<String, Object> atributos);

    // Helper para las implementaciones: si la clave viene informada, debe ser uno de "validos"
    default void siPresenteDebeSerUnoDe(Map<String, Object> atributos, String clave, Set<String> validos) {
        Object v = atributos.get(clave);
        if (v != null && !v.toString().isBlank() && !validos.contains(v.toString()))
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "\"" + v + "\" no es un valor válido para \"" + clave + "\"");
    }
}
