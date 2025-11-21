package com.ecommerce.tiendaspring.controllers;

import com.ecommerce.tiendaspring.dto.PasswordChangeDto;
import com.ecommerce.tiendaspring.models.Rol;
import com.ecommerce.tiendaspring.models.Usuario;
import com.ecommerce.tiendaspring.services.CarritoService;
import com.ecommerce.tiendaspring.services.UsuarioService;
import com.ecommerce.tiendaspring.repositories.RolRepository;
import com.ecommerce.tiendaspring.models.CarritoItemDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.web.servlet.support.RequestContextUtils;

@Controller
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CarritoService carritoService;

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @GetMapping("/login-success")
    public String loginSuccessHandler(HttpServletRequest request, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
         boolean esAdmin = authentication.getAuthorities()
            .stream()
            .anyMatch(rol -> rol.getAuthority().equals("ROLE_ADMIN"));

        // Limpiar carrito de sesión anterior
        request.getSession().setAttribute("carrito", new ArrayList<CarritoItemDTO>());

        try {
            // Cargar carrito del usuario desde BD
            List<CarritoItemDTO> carritoBD = carritoService.cargarCarritoUsuario(email);

            if (!carritoBD.isEmpty()) {
                carritoService.sincronizarStockCarrito(carritoBD);
                request.getSession().setAttribute("carrito", carritoBD);
            }

        } catch (Exception e) {
            request.getSession().setAttribute("carrito", new ArrayList<CarritoItemDTO>());
        }

        if (esAdmin) {
            return "redirect:/admin";
        }

        // Mostrar mensaje si el admin restableció la contraseña recientemente
        try {
            usuarioService.obtenerUsuarioPorEmail(email).ifPresent(u -> {
                if (Boolean.TRUE.equals(u.getPasswordRestablecida())) {
                    // añadir flash attribute para la siguiente petición
                    RequestContextUtils.getOutputFlashMap(request).put("success", "Has ingresado con contraseña restablecida. Cámbiala ya.");
                    // limpiar la bandera
                    u.setPasswordRestablecida(false);
                    usuarioService.guardarUsuario(u);
                }
            });
        } catch (Exception ignore) {}
        return "redirect:/tienda";
    }

    @GetMapping("/register")
    public String mostrarRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "register";
    }

    @PostMapping("/register")
    public String registrarUsuario(@ModelAttribute Usuario usuario, Model model) {
        if (usuarioService.existeUsuarioPorEmail(usuario.getEmail())) {
            model.addAttribute("error", "El email ya está registrado");
            return "register";
        }

        if (!usuario.getEmail().toLowerCase().endsWith("@gmail.com")) {
            model.addAttribute("error", "Solo se aceptan cuentas de Gmail (@gmail.com)");
            return "register";
        }

        if (usuario.getNombre() == null || usuario.getEmail() == null || usuario.getPassword() == null ||
            usuario.getNombre().trim().isEmpty() || usuario.getEmail().trim().isEmpty() || usuario.getPassword().trim().isEmpty()) {
            model.addAttribute("error", "Todos los campos son obligatorios");
            return "register";
        }

        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        Rol rolUsuario = rolRepository.findByNombre("ROLE_USER").orElseGet(() -> rolRepository.save(new Rol("ROLE_USER")));
        usuario.setRoles(Collections.singletonList(rolUsuario));
        usuarioService.guardarUsuario(usuario);

        model.addAttribute("success", "Registro exitoso. Ahora puedes iniciar sesión.");
        return "login";
    }

    @GetMapping("/change-password")
    public String showChangePasswordForm(Model model) {
        model.addAttribute("passwordChangeDto", new PasswordChangeDto());
        return "change-password";
    }

    @PostMapping("/change-password")
    public String processPasswordChange(@ModelAttribute PasswordChangeDto passwordChangeDto, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        if (!passwordChangeDto.getNewPassword().equals(passwordChangeDto.getConfirmPassword())) {
            model.addAttribute("error", "Las nuevas contraseñas no coinciden");
            return "change-password";
        }

        if (passwordChangeDto.getNewPassword().length() < 8) {
            model.addAttribute("error", "La nueva contraseña debe tener al menos 8 caracteres");
            return "change-password";
        }

        boolean passwordChanged = usuarioService.changePassword(
                email, passwordChangeDto.getCurrentPassword(),
                passwordChangeDto.getNewPassword(), passwordEncoder
        );

        if (passwordChanged) {
            model.addAttribute("success", "Contraseña cambiada exitosamente");
        } else {
            model.addAttribute("error", "La contraseña actual es incorrecta");
        }

        return "change-password";
    }
}
