package com.syntia.mvp.controller;

import com.syntia.mvp.model.Rol;
import com.syntia.mvp.model.Usuario;
import com.syntia.mvp.model.dto.RegistroDTO;
import com.syntia.mvp.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controlador de autenticación y navegación principal.
 * Gestiona el login, la redirección por rol y las rutas de los dashboards.
 */
@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Muestra el formulario de registro.
     */
    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("registroDTO", new RegistroDTO());
        return "registro";
    }

    /**
     * Procesa el formulario de registro.
     */
    @PostMapping("/registro")
    public String procesarRegistro(@Valid @ModelAttribute("registroDTO") RegistroDTO dto,
                                   BindingResult result,
                                   Model model) {
        // Validaciones del formulario
        if (result.hasErrors()) {
            return "registro";
        }

        if (!dto.getPassword().equals(dto.getConfirmarPassword())) {
            model.addAttribute("errorPassword", "Las contraseñas no coinciden");
            return "registro";
        }

        try {
            usuarioService.registrar(dto.getEmail(), dto.getPassword(), Rol.USUARIO);
        } catch (IllegalStateException e) {
            model.addAttribute("errorEmail", e.getMessage());
            return "registro";
        }

        return "redirect:/login?registro=true";
    }

    /**
     * Muestra la página de login.
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Redirige la raíz al login.
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    /**
     * Dashboard del administrador.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Authentication authentication, Model model) {
        addUsuarioToModel(authentication, model);
        return "admin/dashboard";
    }

    /**
     * Dashboard del usuario final.
     */
    @PreAuthorize("hasRole('USUARIO')")
    @GetMapping("/usuario/dashboard")
    public String userDashboard(Authentication authentication, Model model) {
        addUsuarioToModel(authentication, model);
        return "usuario/dashboard";
    }

    /**
     * Redirige a la página de inicio según el rol del usuario autenticado.
     *
     * @param authentication datos de autenticación del usuario
     * @return redirección al dashboard correspondiente
     */
    @RequestMapping("/default")
    public String defaultAfterLogin(Authentication authentication) {
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USUARIO"))) {
            return "redirect:/usuario/dashboard";
        }
        return "redirect:/login";
    }

    /**
     * Añade el objeto Usuario al modelo para las vistas Thymeleaf.
     */
    private void addUsuarioToModel(Authentication authentication, Model model) {
        String email = authentication.getName();
        Usuario usuario = usuarioService.buscarPorEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
        model.addAttribute("usuario", usuario);
    }
}
