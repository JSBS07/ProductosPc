package com.ecommerce.tiendaspring.services;

import com.ecommerce.tiendaspring.models.Producto;
import com.ecommerce.tiendaspring.repositories.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class StockService {

    @Autowired
    private ProductoRepository productoRepository;

    // Reservar stock cuando se agrega al carrito
    public synchronized boolean reservarStock(Long productoId, int cantidad) {
        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();
            int stockDisponible = producto.getStock() - producto.getStockReservado();

            // Verificar si hay stock disponible para reservar
            if (stockDisponible >= cantidad) {
                producto.setStockReservado(producto.getStockReservado() + cantidad);
                productoRepository.save(producto);
                return true;
            }
        }
        return false;
    }

    // Liberar stock cuando se elimina del carrito
    public synchronized void liberarStock(Long productoId, int cantidad) {
        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();

            // Evitar valores negativos
            int nuevoStockReservado = producto.getStockReservado() - cantidad;
            producto.setStockReservado(Math.max(0, nuevoStockReservado));

            productoRepository.save(producto);
        }
    }

    // Actualizar reserva cuando se modifica la cantidad en el carrito
    public synchronized boolean actualizarReserva(Long productoId, int cantidadAnterior, int cantidadNueva) {
        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();

            int stockDisponible = producto.getStock() - producto.getStockReservado();
            int diferencia = cantidadNueva - cantidadAnterior;

            // Si se aumenta la cantidad, verificar disponibilidad
            if (diferencia > 0 && stockDisponible < diferencia) {
                return false;
            }

            producto.setStockReservado(producto.getStockReservado() + diferencia);
            productoRepository.save(producto);
            return true;
        }
        return false;
    }

    // Confirmar venta - convertir stock reservado en vendido
    public synchronized void confirmarVenta(Long productoId, int cantidad) {
        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();

            // Reducir stock reservado y stock real
            producto.setStockReservado(producto.getStockReservado() - cantidad);
            producto.setStock(producto.getStock() - cantidad);

            productoRepository.save(producto);
        }
    }

    // Limpiar reservas de stock para un usuario específico
    public void limpiarReservasUsuario(Long usuarioId) {
        // Esta función se llamará cuando el usuario cierre sesión
        // o cuando el carrito se elimine
        System.out.println("Limpiando reservas para usuario: " + usuarioId);
    }

    // Limpiar las reservas expiradas (para un cron job futuro)
    public void limpiarReservasExpiradas() {
        // Podrías implementar esto más adelante para limpiar carritos abandonados
        System.out.println("Limpiando reservas expiradas");
    }
}
