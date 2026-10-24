package com.botica.validacion;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class ValidadorPanales implements ValidadorAtributos {
    private static final Set<String> TALLAS = Set.of("RN", "RN+", "S", "M", "L", "XL", "XXL");
    private static final Set<String> ETAPAS = Set.of("Recién nacido", "Bebé", "Gateador", "Caminador", "Niño", "Adulto");

    @Override public String categoria() { return "PANALES"; }

    @Override
    public void validar(Map<String, Object> atributos) {
        siPresenteDebeSerUnoDe(atributos, "talla", TALLAS);
        siPresenteDebeSerUnoDe(atributos, "etapa", ETAPAS);
    }
}
