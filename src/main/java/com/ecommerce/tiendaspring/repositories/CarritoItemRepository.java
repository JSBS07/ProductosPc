package com.ecommerce.tiendaspring.repositories;

import com.ecommerce.tiendaspring.models.CarritoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarritoItemRepository extends JpaRepository<CarritoItem, Long> {
    void deleteByCarritoId(Long carritoId);
}