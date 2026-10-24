package com.botica.validacion;

import com.botica.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class ValidadorBiberon implements ValidadorAtributos {
    private static final Set<String> UNIDADES = Set.of("mL", "oz");
    private static final Set<String> MATERIALES = Set.of("Plástico", "Vidrio", "Silicona");

    @Override public String categoria() { return "BIBERON"; }

    @Override
    public void validar(Map<String, Object> atributos) {
        Object capacidad = atributos.get("capacidad");
        if (capacidad != null && !capacidad.toString().isBlank()) {
            try {
                if (Double.parseDouble(capacidad.toString()) <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "\"capacidad\" debe ser un número mayor a 0");
            }
        }
        siPresenteDebeSerUnoDe(atributos, "unidadCapacidad", UNIDADES);
        siPresenteDebeSerUnoDe(atributos, "material", MATERIALES);
    }
}
