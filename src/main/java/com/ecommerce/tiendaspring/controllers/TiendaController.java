package com.ecommerce.tiendaspring.controllers;

import com.ecommerce.tiendaspring.models.CarritoItemDTO;
import com.ecommerce.tiendaspring.models.Producto;
import com.ecommerce.tiendaspring.services.ProductoService;
import com.ecommerce.tiendaspring.services.CarritoService;
import com.ecommerce.tiendaspring.services.StockService;
// UsuarioService no es necesario en este controlador (guardado usa email desde auth)
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
// java.util.Optional removido (no usado)

@Controller
@SessionAttributes("carrito")
public class TiendaController {

    @Autowired
    private ProductoService productoService;

    // No reservar stock al agregar al carrito: no inyectamos StockService aquí

    @Autowired
    private CarritoService carritoService;

    @Autowired
    private StockService stockService;
    // UsuarioService removido

    // helper para usuario actual no usado en este controlador

    // ================= Inicializar carrito =================
    @ModelAttribute("carrito")
    public List<CarritoItemDTO> inicializarCarrito() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                List<CarritoItemDTO> carritoBD = carritoService.cargarCarritoUsuario(auth.getName());
                if (carritoBD != null && !carritoBD.isEmpty()) {
                    carritoService.sincronizarStockCarrito(carritoBD);
                    return carritoBD;
                }
            }
        } catch (Exception e) {
            System.out.println("Error cargando carrito desde BD: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    // ================= Mostrar tienda =================
    @GetMapping("/tienda")
    public String mostrarTienda(@RequestParam(defaultValue = "todos") String categoria,
                                @ModelAttribute("carrito") List<CarritoItemDTO> carrito,
                                HttpSession session,
                                Model model) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                String email = auth.getName();
                carritoService.guardarCarritoUsuario(email, carrito);
            }

            List<Producto> productos = "todos".equals(categoria)
                    ? productoService.obtenerProductosDisponibles()
                    : productoService.obtenerProductosDisponiblesPorCategoria(categoria);

            model.addAttribute("productos", productos);
            model.addAttribute("categoriaSeleccionada", categoria);

            return "tienda";

        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar la tienda: " + e.getMessage());
            return "tienda";
        }
    }

    // ================= Agregar al carrito =================
    @PostMapping("/agregar-carrito")
    public String agregarAlCarrito(@RequestParam Long productoId,
                                   @RequestParam(defaultValue = "1") int cantidad,
                                   @RequestParam(defaultValue = "todos") String categoria,
                                   @ModelAttribute("carrito") List<CarritoItemDTO> carrito,
                                   Model model) {
        try {
            Producto producto = productoService.obtenerProductoPorId(productoId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            // No reservamos stock en BD al agregar al carrito. Hacemos una validación local
            // para evitar que el mismo usuario agregue más de lo que hay en `stock`.
            CarritoItemDTO itemExistente = carrito.stream()
                    .filter(i -> i.getProducto().getId().equals(productoId))
                    .findFirst()
                    .orElse(null);

            if (itemExistente != null) {
                int cantidadAnterior = itemExistente.getCantidad();
                int nuevaCantidadTotal = cantidadAnterior + cantidad;
                // intentar actualizar reserva (atomico)
                if (!stockService.actualizarReserva(productoId, cantidadAnterior, nuevaCantidadTotal)) {
                    model.addAttribute("error", "No hay suficiente stock. Solo quedan " + producto.getStockDisponible() + " unidades.");
                    return "redirect:/tienda?categoria=" + categoria;
                }
                itemExistente.setCantidad(nuevaCantidadTotal);
                // actualizar referencia de producto con estado actual
                itemExistente.setProducto(producto);
            } else {
                if (!stockService.reservarStock(productoId, cantidad)) {
                    model.addAttribute("error", "No hay suficiente stock. Solo quedan " + producto.getStockDisponible() + " unidades.");
                    return "redirect:/tienda?categoria=" + categoria;
                }
                carrito.add(new CarritoItemDTO(producto, cantidad));
            }

            // Guardar carrito en BD si usuario está autenticado
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                carritoService.guardarCarritoUsuario(auth.getName(), carrito);
            }

            model.addAttribute("success", "Producto agregado al carrito");

        } catch (Exception e) {
            model.addAttribute("error", "Error al agregar producto: " + e.getMessage());
        }

        return "redirect:/tienda?categoria=" + categoria;
    }

    // ================= Ver carrito =================
    @GetMapping("/carrito")
    public String verCarrito(@ModelAttribute("carrito") List<CarritoItemDTO> carrito, Model model) {
        model.addAttribute("total", carrito.stream()
                .map(CarritoItemDTO::getSubtotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));

        return "carrito";
    }

    // ================= Actualizar carrito =================
    @PostMapping("/actualizar-carrito")
    public String actualizarCarrito(@RequestParam Long productoId,
                                    @RequestParam int cantidad,
                                    @ModelAttribute("carrito") List<CarritoItemDTO> carrito,
                                    Model model) {
        try {
            Iterator<CarritoItemDTO> it = carrito.iterator();
            while (it.hasNext()) {
                CarritoItemDTO item = it.next();
                if (item.getProducto().getId().equals(productoId)) {
                    //int cantidadAnterior = item.getCantidad();
                    if (cantidad <= 0) {
                        // liberar reserva y remover
                        int cantidadAnterior = item.getCantidad();
                        stockService.liberarStock(productoId, cantidadAnterior);
                        it.remove();
                        model.addAttribute("success", "Producto eliminado");
                    } else {
                        // actualizar reserva de forma atomica
                        int cantidadAnterior = item.getCantidad();
                        if (!stockService.actualizarReserva(productoId, cantidadAnterior, cantidad)) {
                            model.addAttribute("error", "No hay suficiente stock para actualizar cantidad");
                            return "redirect:/carrito";
                        }
                        item.setCantidad(cantidad);
                        model.addAttribute("success", "Cantidad actualizada");
                    }
                    break;
                }
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                carritoService.guardarCarritoUsuario(auth.getName(), carrito);
            }

        } catch (Exception e) {
            model.addAttribute("error", "Error al actualizar carrito: " + e.getMessage());
        }

        return "redirect:/carrito";
    }

    // ================= Eliminar item del carrito =================
    @PostMapping("/eliminar-carrito")
    public String eliminarCarrito(@RequestParam Long productoId,
                                 @ModelAttribute("carrito") List<CarritoItemDTO> carrito,
                                 Model model) {
        Iterator<CarritoItemDTO> it = carrito.iterator();
        while (it.hasNext()) {
            CarritoItemDTO item = it.next();
            if (item.getProducto().getId().equals(productoId)) {
                // liberar reserva antes de remover
                stockService.liberarStock(productoId, item.getCantidad());
                it.remove();
                break;
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            carritoService.guardarCarritoUsuario(auth.getName(), carrito);
        }

        model.addAttribute("success", "Producto eliminado");
        return "redirect:/carrito";
    }

    // ================= Vaciar carrito =================
    @GetMapping("/vaciar-carrito")
    public String vaciarCarrito(@ModelAttribute("carrito") List<CarritoItemDTO> carrito, Model model) {
        for (CarritoItemDTO item : carrito) {
            stockService.liberarStock(item.getProducto().getId(), item.getCantidad());
        }
        carrito.clear();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            carritoService.guardarCarritoUsuario(auth.getName(), carrito);
        }

        model.addAttribute("success", "Carrito vaciado");
        return "redirect:/carrito";
    }
}
