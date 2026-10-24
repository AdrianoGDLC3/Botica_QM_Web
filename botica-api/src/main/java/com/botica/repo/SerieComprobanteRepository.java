package com.botica.repo;

import com.botica.domain.SerieComprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SerieComprobanteRepository extends JpaRepository<SerieComprobante, String> {

    // Upsert atómico: crea la serie si no existe (arranca en 1) o incrementa la existente, y
    // devuelve el correlativo resultante. Al ser una sola sentencia es seguro con varias ventas
    // concurrentes o varias instancias de la app corriendo contra la misma base.
    @Query(value = "INSERT INTO serie_comprobante (serie, correlativo) VALUES (:serie, 1) " +
            "ON CONFLICT (serie) DO UPDATE SET correlativo = serie_comprobante.correlativo + 1 " +
            "RETURNING correlativo", nativeQuery = true)
    Long incrementarYObtener(@Param("serie") String serie);
}
