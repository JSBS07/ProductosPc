package com.ecommerce.tiendaspring.services;

import com.ecommerce.tiendaspring.models.*;
import com.ecommerce.tiendaspring.repositories.CarritoRepository;
import com.ecommerce.tiendaspring.repositories.CarritoItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private UsuarioService usuarioService;

    @Autowired
    private StockService stockService;

    // ================= Obtener o crear carrito para un usuario =================
    public Carrito obtenerCarritoUsuario(Usuario usuario) {
        return carritoRepository.findByUsuario(usuario)
                .orElseGet(() -> {
                    Carrito nuevoCarrito = new Carrito(usuario);
                    return carritoRepository.save(nuevoCarrito);
                });
    }

    // ================= Convertir Carrito persistente a DTO =================
    public List<CarritoItemDTO> convertirACarritoDTO(Carrito carrito) {
        List<CarritoItemDTO> carritoDTO = new ArrayList<>();
        for (CarritoItem item : carrito.getItems()) {
            carritoDTO.add(new CarritoItemDTO(item.getProducto(), item.getCantidad()));
        }
        return carritoDTO;
    }

    // ================= Actualizar Carrito desde DTO =================
    public void actualizarCarritoDesdeDTO(Usuario usuario, List<CarritoItemDTO> carritoDTO) {
        Carrito carrito = obtenerCarritoUsuario(usuario);

        // Limpiar items existentes
        carrito.getItems().clear();
        carritoItemRepository.deleteByCarritoId(carrito.getId());

        // Agregar nuevos items desde DTO
        for (CarritoItemDTO itemDTO : carritoDTO) {
            CarritoItem item = new CarritoItem(carrito, itemDTO.getProducto(), itemDTO.getCantidad());
            carrito.agregarItem(item);
        }

        carritoRepository.save(carrito);
    }

    // ================= Cargar carrito desde BD =================
    public List<CarritoItemDTO> cargarCarritoUsuario(String email) {
        System.out.println("Cargando carrito para usuario: " + email);

        Optional<Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorEmail(email);
        if (usuarioOpt.isPresent()) {
            Carrito carrito = obtenerCarritoUsuario(usuarioOpt.get());
            List<CarritoItemDTO> carritoDTO = convertirACarritoDTO(carrito);
            System.out.println("Carrito cargado: " + carritoDTO.size() + " items");
            return carritoDTO;
        }

        System.out.println("Usuario no encontrado: " + email);
        return new ArrayList<>();
    }

    // ================= Guardar carrito DTO en BD =================
    public void guardarCarritoUsuario(String email, List<CarritoItemDTO> carritoDTO) {
        System.out.println("Guardando carrito para usuario: " + email + " - Items: " + carritoDTO.size());

        Optional<Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorEmail(email);
        if (usuarioOpt.isPresent()) {
            actualizarCarritoDesdeDTO(usuarioOpt.get(), carritoDTO);
            System.out.println("Carrito guardado exitosamente");
            // Después de guardar en BD, emitir actualizaciones de stock para cada producto
            for (CarritoItemDTO item : carritoDTO) {
                try {
                    stockService.broadcastStockUpdate(item.getProducto().getId());
                } catch (Exception e) {
                    System.out.println("Error broadcasting stock update: " + e.getMessage());
                }
            }
        } else {
            System.out.println("No se pudo guardar carrito - usuario no encontrado");
        }
    }

    // ================= Sincronizar stock entre sesión y BD =================
    public void sincronizarStockCarrito(List<CarritoItemDTO> carritoDTO) {
        for (CarritoItemDTO item : carritoDTO) {
            int stockDisponible = item.getProducto().getStockDisponible();
            if (item.getCantidad() > stockDisponible) {
                System.out.println("Ajustando cantidad de " + item.getProducto().getNombre() +
                        " de " + item.getCantidad() + " a " + stockDisponible);
                item.setCantidad(stockDisponible);
            }
        }
    }
}
