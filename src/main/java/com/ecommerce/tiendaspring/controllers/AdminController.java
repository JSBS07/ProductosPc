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

    @GetMapping({"","/","/dashboard"})
    public String dashboard(Model model) {
        List<Producto> productos = productoService.obtenerTodosLosProductos();
        model.addAttribute("productos", productos);
        return "admin-dashboard";
    }

    @GetMapping("/productos")
    public String listarProductos(Model model) {
        model.addAttribute("productos", productoService.obtenerTodosLosProductos());
        return "admin-productos";
    }

    @GetMapping("/productos/reponer/{id}")
    public String formReponer(@PathVariable Long id, Model model) {
        Producto p = productoService.obtenerProductoPorId(id).orElse(null);
        if (p == null) {
            model.addAttribute("error", "Producto no encontrado");
            return "redirect:/admin/productos";
        }
        model.addAttribute("producto", p);
        return "admin-reponer-stock";
    }

    @PostMapping("/productos/reponer/{id}")
    public String reponerStock(@PathVariable Long id,
                               @RequestParam(name = "cantidad") Integer cantidad,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            if (cantidad == null || cantidad <= 0) {
                redirectAttributes.addFlashAttribute("error", "Cantidad inválida");
                return "redirect:/admin/productos";
            }
            // Llamamos al servicio que ya limita la cantidad a 30
            productoService.reponerStock(id, cantidad);
            if (cantidad > 30) {
                redirectAttributes.addFlashAttribute("success", "Cantidad limitada a 30. Stock repuesto correctamente.");
            } else {
                redirectAttributes.addFlashAttribute("success", "Stock repuesto correctamente");
            }
            return "redirect:/admin/productos";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al reponer stock: " + e.getMessage());
            return "redirect:/admin/productos";
        }
    }

    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll());
        return "admin-usuarios";
    }

    @PostMapping("/usuarios/reset/{id}")
    public String resetearContrasena(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Fijar contraseña temporal 123456 como pediste
            // obtener email del usuario para el mensaje
            String email = usuarioRepository.findById(id).map(u -> u.getEmail()).orElse("(desconocido)");
            usuarioService.resetPasswordByAdmin(id, "123456", passwordEncoder);
            redirectAttributes.addFlashAttribute("success", "Has cambiado la contraseña para " + email + ". La nueva es 123456");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al restablecer contraseña: " + e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }
}