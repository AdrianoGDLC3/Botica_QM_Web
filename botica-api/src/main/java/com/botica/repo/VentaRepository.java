package com.botica.repo;

import com.botica.domain.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Query("select v from Venta v left join fetch v.cliente join fetch v.empleado order by v.fechaVenta desc")
    List<Venta> listar();

    @Query("select v from Venta v left join fetch v.cliente join fetch v.empleado " +
           "left join fetch v.detalles d left join fetch d.lote l left join fetch l.producto where v.id = :id")
    Optional<Venta> detalle(@Param("id") Long id);
}
