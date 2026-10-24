package com.botica.util;

public final class Textos {
    private Textos() {}

    // '' o null -> null (útil para columnas opcionales, evita guardar cadenas vacías)
    public static String opcional(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
