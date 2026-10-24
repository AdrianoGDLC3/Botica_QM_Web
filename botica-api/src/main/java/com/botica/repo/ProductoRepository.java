package com.botica.repo;

import com.botica.domain.Producto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    @EntityGraph(attributePaths = "laboratorios")
    List<Producto> findAllByOrderByNombreAsc();
}
