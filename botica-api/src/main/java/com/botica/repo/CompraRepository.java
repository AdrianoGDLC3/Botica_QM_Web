package com.botica.repo;

import com.botica.domain.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    // Para decidir si un proveedor se puede eliminar (ver ProveedorService)
    @Query("select count(c) from Compra c where c.proveedor.id = :proveedorId")
    long contarPorProveedor(@Param("proveedorId") Long proveedorId);

    @Query("select c from Compra c join fetch c.proveedor join fetch c.usuario order by c.fechaCompra desc")
    List<Compra> listar();

    @Query("select c from Compra c join fetch c.proveedor join fetch c.usuario " +
           "left join fetch c.detalles d left join fetch d.lote l left join fetch l.producto left join fetch l.laboratorio where c.id = :id")
    Optional<Compra> detalle(@Param("id") Long id);
}
