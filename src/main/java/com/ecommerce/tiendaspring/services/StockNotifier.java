package com.ecommerce.tiendaspring.services;

import com.ecommerce.tiendaspring.models.Producto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StockNotifier {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notificarCambioStock(Producto producto) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("productoId", producto.getId());
        payload.put("stock", producto.getStock());
        payload.put("stockReservado", producto.getStockReservado());

        messagingTemplate.convertAndSend("/topic/stock", payload);
    }
}