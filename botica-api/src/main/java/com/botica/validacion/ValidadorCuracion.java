package com.botica.validacion;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class ValidadorCuracion implements ValidadorAtributos {
    private static final Set<String> TIPOS = Set.of("Algodón", "Gasa", "Venda", "Esparadrapo", "Curitas", "Guantes");

    @Override public String categoria() { return "CURACION"; }

    @Override
    public void validar(Map<String, Object> atributos) {
        siPresenteDebeSerUnoDe(atributos, "tipo", TIPOS);
    }
}
