package com.ecommerce.tiendaspring.services;

import com.ecommerce.tiendaspring.models.Producto;
import com.ecommerce.tiendaspring.models.StockUpdate;
import com.ecommerce.tiendaspring.repositories.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class StockService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private com.ecommerce.tiendaspring.repositories.CarritoItemRepository carritoItemRepository;

    // Reservar stock cuando se agrega al carrito
    public synchronized boolean reservarStock(Long productoId, int cantidad) {
        // Este método ya no modifica product.stockReservado directamente.
        // Las reservas se almacenan como CarritoItem en BD; la comprobación de disponibilidad
        // se realiza leyendo la suma de cantidades reservadas en CarritoItem.
        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();
            Integer reservado = carritoItemRepository.sumCantidadByProductoId(productoId);
            int reservadoVal = (reservado == null) ? 0 : reservado;
            int stockDisponible = producto.getStock() - reservadoVal;
            return stockDisponible >= cantidad;
        }
        return false;
    }

    // Liberar stock cuando se elimina del carrito
    public synchronized void liberarStock(Long productoId, int cantidad) {
        // Ya no modificamos product.stockReservado aquí. Eliminar reservas significa
        // eliminar/actualizar CarritoItem en la BD (manejado por CarritoService).
        // Aún así, publicamos la actualización de stock para que clientes la reciban.
        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();
            Integer reservado = carritoItemRepository.sumCantidadByProductoId(productoId);
            int reservadoVal = (reservado == null) ? 0 : reservado;
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/stock",
                        new StockUpdate(producto.getId(), producto.getStock(), reservadoVal));
            }
        }
    }

    // Actualizar reserva cuando se modifica la cantidad en el carrito
    public synchronized boolean actualizarReserva(Long productoId, int cantidadAnterior, int cantidadNueva) {
        // Validar disponibilidad usando la suma de CarritoItems.
        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();
            Integer reservado = carritoItemRepository.sumCantidadByProductoId(productoId);
            int reservadoVal = (reservado == null) ? 0 : reservado;
            int stockDisponible = producto.getStock() - reservadoVal;
            int diferencia = cantidadNueva - cantidadAnterior;
            if (diferencia > 0 && stockDisponible < diferencia) {
                return false;
            }
            // No escribimos en producto.stockReservado; la persistencia se hace via CarritoItem.
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/stock",
                        new StockUpdate(producto.getId(), producto.getStock(), reservadoVal));
            }
            return true;
        }
        return false;
    }

    // Confirmar venta - convertir stock reservado en vendido
    public synchronized void confirmarVenta(Long productoId, int cantidad) {
        // Al confirmar venta, reducimos el stock real. Las reservas (CarritoItems)
        // deben ser eliminadas por la lógica de checkout que persistió los cambios.
        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();
            int nuevoStock = producto.getStock() - cantidad;
            producto.setStock(Math.max(0, nuevoStock));
            productoRepository.save(producto);
            Integer reservado = carritoItemRepository.sumCantidadByProductoId(productoId);
            int reservadoVal = (reservado == null) ? 0 : reservado;
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/stock",
                        new StockUpdate(producto.getId(), producto.getStock(), reservadoVal));
            }
        }
    }

    // Calcula y publica el estado actual de stock para un producto
    public void broadcastStockUpdate(Long productoId) {
        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();
            Integer reservado = carritoItemRepository.sumCantidadByProductoId(productoId);
            int reservadoVal = (reservado == null) ? 0 : reservado;
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/stock",
                        new StockUpdate(producto.getId(), producto.getStock(), reservadoVal));
            }
        }
    }

    public int getStockDisponible(Long productoId) {
        Optional<Producto> productoOpt = productoRepository.findById(productoId);
        if (productoOpt.isPresent()) {
            Producto producto = productoOpt.get();
            Integer reservado = carritoItemRepository.sumCantidadByProductoId(productoId);
            int reservadoVal = (reservado == null) ? 0 : reservado;
            return producto.getStock() - reservadoVal;
        }
        return 0;
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
