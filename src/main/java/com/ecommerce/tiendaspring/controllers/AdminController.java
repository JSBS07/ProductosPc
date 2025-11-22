package com.ecommerce.tiendaspring.controllers;

import com.ecommerce.tiendaspring.models.Producto;
import com.ecommerce.tiendaspring.models.Usuario;
import com.ecommerce.tiendaspring.repositories.UsuarioRepository;
import com.ecommerce.tiendaspring.services.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private com.ecommerce.tiendaspring.services.UsuarioService usuarioService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // VISTA PRINCIPAL 
    @GetMapping({"","/","/dashboard"})
    public String dashboard(Model model) {
        List<Producto> productos = productoService.obtenerTodosLosProductos();
        
        //Cálculos para las tarjetas del dashboard
        long totalProductos = productos.size();
        long stockBajo = productos.stream()
            .filter(p -> {
                int stockReal = p.getStock() - p.getStockReservado();
                return stockReal > 2 && stockReal <= 5;
            })
            .count();
        long stockCritico = productos.stream()
            .filter(p -> {
                int stockReal = p.getStock() - p.getStockReservado();
                return stockReal <= 2;
            })
            .count();
        int totalReservado = productos.stream()
            .mapToInt(Producto::getStockReservado)
            .sum();
        int inventarioTotal = productos.stream()
            .mapToInt(Producto::getStock)
            .sum();
        int disponibleReal = inventarioTotal - totalReservado;
        
        model.addAttribute("productos", productos);
        model.addAttribute("totalProductos", totalProductos);
        model.addAttribute("stockBajo", stockBajo);
        model.addAttribute("stockCritico", stockCritico);
        model.addAttribute("totalReservado", totalReservado);
        model.addAttribute("inventarioTotal", inventarioTotal);
        model.addAttribute("disponibleReal", disponibleReal);
        
        return "admin-productos";
    }

    // Reposición rápida desde la tabla
    @PostMapping("/productos/reponer/{id}")
    public String reponerStock(@PathVariable Long id,
                               @RequestParam(name = "cantidad") Integer cantidad,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            if (cantidad == null || cantidad <= 0) {
                redirectAttributes.addFlashAttribute("error", "Cantidad inválida");
                return "redirect:/admin"; 
            }
            
            //  limita la cantidad a 30
            productoService.reponerStock(id, cantidad);
            
            if (cantidad > 30) {
                redirectAttributes.addFlashAttribute("success", "Cantidad limitada a 30. Stock repuesto correctamente.");
            } else {
                redirectAttributes.addFlashAttribute("success", "Stock repuesto correctamente: +" + cantidad + " unidades");
            }
            return "redirect:/admin"; 
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al reponer stock: " + e.getMessage());
            return "redirect:/admin"; 
        }
    }

    //  Gestión de usuarios
    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll());
        return "admin-usuarios";
    }

    @PostMapping("/usuarios/reset/{id}")
    public String resetearContrasena(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Fijar contraseña temporal 123456 
            String email = usuarioRepository.findById(id).map(u -> u.getEmail()).orElse("(desconocido)");
            usuarioService.resetPasswordByAdmin(id, "123456", passwordEncoder);
            redirectAttributes.addFlashAttribute("success", "Has cambiado la contraseña para " + email + ". La nueva es 123456");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al restablecer contraseña: " + e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }
}