package com.ecommerce.tiendaspring.repositories;

import com.ecommerce.tiendaspring.models.CarritoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CarritoItemRepository extends JpaRepository<CarritoItem, Long> {
    void deleteByCarritoId(Long carritoId);

    @Query("SELECT COALESCE(SUM(ci.cantidad), 0) FROM CarritoItem ci WHERE ci.producto.id = :productoId")
    Integer sumCantidadByProductoId(@Param("productoId") Long productoId);
}