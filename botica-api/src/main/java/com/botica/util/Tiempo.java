package com.botica.util;

import com.botica.config.ConfiguracionNegocio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class Tiempo {
    private Tiempo() {}

    // La zona horaria sale del Singleton de configuración (ver config.ConfiguracionNegocio)
    private static ZoneId zona() { return ConfiguracionNegocio.instancia().zonaHoraria(); }

    public static LocalDateTime ahora() { return LocalDateTime.now(zona()); }
    public static LocalDate hoy() { return LocalDate.now(zona()); }
}
