package com.botica.validacion;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class ValidadorHigiene implements ValidadorAtributos {
    private static final Set<String> TIPOS = Set.of("Jabón", "Alcohol", "Gel antibacterial", "Shampoo",
            "Desodorante", "Pasta dental", "Enjuague bucal", "Toallas húmedas");

    @Override public String categoria() { return "HIGIENE"; }

    @Override
    public void validar(Map<String, Object> atributos) {
        siPresenteDebeSerUnoDe(atributos, "tipo", TIPOS);
    }
}
