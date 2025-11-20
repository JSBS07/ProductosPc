package com.ecommerce.tiendaspring.services;

import com.ecommerce.tiendaspring.models.*;
import com.ecommerce.tiendaspring.repositories.CarritoRepository;
import com.ecommerce.tiendaspring.repositories.CarritoItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CarritoService {

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private CarritoItemRepository carritoItemRepository;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private StockService stockService;

    // Obtener o crear carrito para un usuario
    public Carrito obtenerCarritoUsuario(Usuario usuario) {
        return carritoRepository.findByUsuario(usuario)
                .orElseGet(() -> {
                    Carrito nuevoCarrito = new Carrito(usuario);
                    return carritoRepository.save(nuevoCarrito);
                });
    }

    // Agregar producto al carrito
    public boolean agregarProductoAlCarrito(Usuario usuario, Long productoId, Integer cantidad) {
        Carrito carrito = obtenerCarritoUsuario(usuario);
        Producto producto = productoService.obtenerProductoPorId(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // Verificar stock disponible
        if (producto.getStockDisponible() < cantidad) {
            return false;
        }

        // Reservar stock
        if (!stockService.reservarStock(productoId, cantidad)) {
            return false;
        }

        // Buscar si el producto ya está en el carrito
        Optional<CarritoItem> itemExistente = carrito.getItems().stream()
                .filter(item -> item.getProducto().getId().equals(productoId))
                .findFirst();

        if (itemExistente.isPresent()) {
            // Actualizar cantidad existente
            CarritoItem item = itemExistente.get();
            stockService.liberarStock(productoId, item.getCantidad());
            if (!stockService.reservarStock(productoId, cantidad)) {
                return false;
            }
            item.setCantidad(cantidad);
        } else {
            // Agregar nuevo item
            CarritoItem nuevoItem = new CarritoItem(carrito, producto, cantidad);
            carrito.agregarItem(nuevoItem);
        }

        carrito.setFechaActualizacion(java.time.LocalDateTime.now());
        carritoRepository.save(carrito);
        return true;
    }

    // Actualizar cantidad en carrito
    public boolean actualizarCantidad(Usuario usuario, Long productoId, Integer nuevaCantidad) {
        Carrito carrito = obtenerCarritoUsuario(usuario);
        
        CarritoItem item = carrito.getItems().stream()
                .filter(i -> i.getProducto().getId().equals(productoId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Producto no encontrado en carrito"));

        int cantidadAnterior = item.getCantidad();
        
        if (nuevaCantidad <= 0) {
            return eliminarProductoDelCarrito(usuario, productoId);
        }

        // Verificar nuevo stock disponible
        Producto producto = item.getProducto();
        int stockNecesario = nuevaCantidad - cantidadAnterior;
        if (producto.getStockDisponible() < stockNecesario) {
            return false;
        }

        // Actualizar reserva de stock
        stockService.liberarStock(productoId, cantidadAnterior);
        if (!stockService.reservarStock(productoId, nuevaCantidad)) {
            // Revertir a la cantidad anterior si falla
            stockService.reservarStock(productoId, cantidadAnterior);
            return false;
        }

        item.setCantidad(nuevaCantidad);
        carrito.setFechaActualizacion(java.time.LocalDateTime.now());
        carritoRepository.save(carrito);
        return true;
    }

    // Eliminar producto del carrito
    public boolean eliminarProductoDelCarrito(Usuario usuario, Long productoId) {
        Carrito carrito = obtenerCarritoUsuario(usuario);
        
        Optional<CarritoItem> itemOpt = carrito.getItems().stream()
                .filter(i -> i.getProducto().getId().equals(productoId))
                .findFirst();

        if (itemOpt.isPresent()) {
            CarritoItem item = itemOpt.get();
            // Liberar stock reservado
            stockService.liberarStock(productoId, item.getCantidad());
            carrito.removerItem(item);
            carritoItemRepository.delete(item);
            carrito.setFechaActualizacion(java.time.LocalDateTime.now());
            carritoRepository.save(carrito);
            return true;
        }
        return false;
    }

    // Vaciar carrito
    public void vaciarCarrito(Usuario usuario) {
        Carrito carrito = obtenerCarritoUsuario(usuario);
        
        // Liberar stock de todos los items
        for (CarritoItem item : carrito.getItems()) {
            stockService.liberarStock(item.getProducto().getId(), item.getCantidad());
        }
        
        carrito.getItems().clear();
        carrito.setFechaActualizacion(java.time.LocalDateTime.now());
        carritoRepository.save(carrito);
    }

    // Obtener total del carrito
    public BigDecimal obtenerTotalCarrito(Usuario usuario) {
        Carrito carrito = obtenerCarritoUsuario(usuario);
        return carrito.getItems().stream()
                .map(CarritoItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Obtener número de items en carrito
    public int obtenerCantidadItemsCarrito(Usuario usuario) {
        Carrito carrito = obtenerCarritoUsuario(usuario);
        return carrito.getItems().stream()
                .mapToInt(CarritoItem::getCantidad)
                .sum();
    }
}