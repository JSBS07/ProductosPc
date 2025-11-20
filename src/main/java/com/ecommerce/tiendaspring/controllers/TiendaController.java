package com.ecommerce.tiendaspring.controllers;

import com.ecommerce.tiendaspring.models.Usuario;
import com.ecommerce.tiendaspring.models.Carrito;
import com.ecommerce.tiendaspring.models.Producto;
import com.ecommerce.tiendaspring.services.ProductoService;
import com.ecommerce.tiendaspring.services.CarritoService;
import com.ecommerce.tiendaspring.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
public class TiendaController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CarritoService carritoService;

    @Autowired
    private UsuarioService usuarioService;

    // Método helper para obtener usuario actual
    private Usuario obtenerUsuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        // Obtener el usuario real de la base de datos
        Optional<Usuario> usuarioOpt = usuarioService.obtenerUsuarioPorEmail(email);
        if (usuarioOpt.isPresent()) {
            return usuarioOpt.get();
        } else {
            throw new RuntimeException("Usuario no encontrado: " + email);
        }
    }

    @GetMapping("/tienda")
    public String mostrarTienda(@RequestParam(defaultValue = "todos") String categoria, Model model) {
        try {
            List<Producto> productos;
            
            if ("todos".equals(categoria)) {
                productos = productoService.obtenerProductosDisponibles();
            } else {
                productos = productoService.obtenerProductosDisponiblesPorCategoria(categoria);
            }
            
            // Obtener cantidad de items en carrito para el badge
            int cantidadCarrito = carritoService.obtenerCantidadItemsCarrito(obtenerUsuarioActual());
            
            model.addAttribute("productos", productos);
            model.addAttribute("categoriaSeleccionada", categoria);
            model.addAttribute("cantidadCarrito", cantidadCarrito);
            return "tienda";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar la tienda: " + e.getMessage());
            return "tienda";
        }
    }

    @PostMapping("/agregar-carrito")
    public String agregarAlCarrito(@RequestParam Long productoId, 
                                  @RequestParam(defaultValue = "1") int cantidad,
                                  @RequestParam(defaultValue = "todos") String categoria,
                                  Model model) {
        
        try {
            boolean exito = carritoService.agregarProductoAlCarrito(
                obtenerUsuarioActual(), productoId, cantidad);
            
            if (exito) {
                model.addAttribute("success", "Producto agregado al carrito");
            } else {
                model.addAttribute("error", "No hay suficiente stock disponible");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error al agregar producto al carrito: " + e.getMessage());
        }
        
        return "redirect:/tienda?categoria=" + categoria;
    }

    @GetMapping("/carrito")
    public String verCarrito(Model model) {
        try {
            Usuario usuario = obtenerUsuarioActual();
            Carrito carrito = carritoService.obtenerCarritoUsuario(usuario);
            BigDecimal total = carritoService.obtenerTotalCarrito(usuario);
            
            model.addAttribute("carrito", carrito);
            model.addAttribute("total", total);
            return "carrito";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar el carrito: " + e.getMessage());
            return "carrito";
        }
    }

    @PostMapping("/actualizar-carrito")
    public String actualizarCarrito(@RequestParam Long productoId,
                                   @RequestParam int cantidad,
                                   Model model) {
        
        try {
            boolean exito = carritoService.actualizarCantidad(obtenerUsuarioActual(), productoId, cantidad);
            if (exito) {
                model.addAttribute("success", "Cantidad actualizada");
            } else {
                model.addAttribute("error", "No hay suficiente stock para actualizar la cantidad");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error al actualizar carrito: " + e.getMessage());
        }
        
        return "redirect:/carrito";
    }

    @PostMapping("/eliminar-carrito")
    public String eliminarDelCarrito(@RequestParam Long productoId, Model model) {
        
        try {
            boolean exito = carritoService.eliminarProductoDelCarrito(obtenerUsuarioActual(), productoId);
            if (exito) {
                model.addAttribute("success", "Producto eliminado del carrito");
            } else {
                model.addAttribute("error", "Error al eliminar producto del carrito");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error al eliminar producto: " + e.getMessage());
        }
        
        return "redirect:/carrito";
    }

    @GetMapping("/vaciar-carrito")
    public String vaciarCarrito(Model model) {
        
        try {
            carritoService.vaciarCarrito(obtenerUsuarioActual());
            model.addAttribute("success", "Carrito vaciado");
        } catch (Exception e) {
            model.addAttribute("error", "Error al vaciar carrito: " + e.getMessage());
        }
        
        return "redirect:/carrito";
    }
}