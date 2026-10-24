package com.botica.repo;

import com.botica.domain.Laboratorio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LaboratorioRepository extends JpaRepository<Laboratorio, Long> {

    // Quita al laboratorio de la lista de fabricantes de cada producto (tabla de unión). No toca
    // lotes: eso lo impide LaboratorioService antes de llegar aquí.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "delete from producto_laboratorio where laboratorio_id = :id", nativeQuery = true)
    void desvincularDeProductos(@Param("id") Long id);
}
