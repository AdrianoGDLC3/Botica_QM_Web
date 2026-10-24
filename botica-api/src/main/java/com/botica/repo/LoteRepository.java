package com.botica.repo;

import com.botica.domain.Lote;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    @Query("select l from Lote l join fetch l.producto join fetch l.laboratorio order by l.fechaVencimiento")
    List<Lote> listar();

    // FEFO: con stock, no vencidos. El orden final lo decide EstrategiaAsignacionLote (Strategy),
    // no esta query — así se puede cambiar la política sin tocar SQL.
    @Query("select l from Lote l join fetch l.producto join fetch l.laboratorio " +
           "where l.producto.id = :productoId and l.stockActual > 0 and l.fechaVencimiento > :hoy")
    List<Lote> disponibles(@Param("productoId") Long productoId, @Param("hoy") LocalDate hoy);

    // Para decidir si un producto / laboratorio se puede eliminar (ver CatalogoService y LaboratorioService)
    @Query("select count(l) from Lote l where l.producto.id = :productoId")
    long contarPorProducto(@Param("productoId") Long productoId);

    @Query("select coalesce(sum(l.stockActual), 0) from Lote l where l.producto.id = :productoId")
    BigDecimal stockTotalDeProducto(@Param("productoId") Long productoId);

    @Query("select count(l) from Lote l where l.laboratorio.id = :laboratorioId")
    long contarPorLaboratorio(@Param("laboratorioId") Long laboratorioId);

    // Bloqueo de fila (SELECT ... FOR UPDATE) para que ventas/compras simultáneas no descuadren el stock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lote l where l.id = :id")
    Optional<Lote> bloquear(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lote l where l.producto.id = :productoId and l.numeroLote = :num")
    Optional<Lote> bloquearPorNumero(@Param("productoId") Long productoId, @Param("num") String num);
}
