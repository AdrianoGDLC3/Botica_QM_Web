package com.botica.validacion;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

// Cubre MEDICAMENTO y SUPLEMENTO (mismos campos: concentración, forma farmacéutica, principio
// activo). concentración y principio activo son texto libre; solo formaFarmaceutica está
// acotada, igual que el <select> del frontend.
@Component
public class ValidadorMedicamento implements ValidadorAtributos {
    private static final Set<String> FORMAS = Set.of(
            "Tableta", "Tableta recubierta", "Tableta masticable", "Tableta efervescente", "Tableta sublingual",
            "Cápsula", "Cápsula dura", "Cápsula blanda", "Gragea", "Polvo", "Granulado",
            "Jarabe", "Suspensión oral", "Solución oral", "Gotas", "Emulsión oral", "Elixir",
            "Solución inyectable", "Suspensión inyectable", "Polvo para solución inyectable",
            "Crema", "Pomada", "Ungüento", "Gel", "Loción", "Spray", "Aerosol",
            "Óvulo", "Supositorio", "Solución oftálmica", "Gotas óticas", "Solución nasal");

    @Override public String categoria() { return "MEDICAMENTO"; }

    @Override
    public void validar(Map<String, Object> atributos) {
        siPresenteDebeSerUnoDe(atributos, "formaFarmaceutica", FORMAS);
    }
}
