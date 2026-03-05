package com.syntia.mvp.controller;

import com.syntia.mvp.model.Rol;
import com.syntia.mvp.model.Usuario;
import com.syntia.mvp.model.dto.RegistroDTO;
import com.syntia.mvp.service.DashboardService;
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
    private final DashboardService dashboardService;

    public AuthController(UsuarioService usuarioService, DashboardService dashboardService) {
        this.usuarioService = usuarioService;
        this.dashboardService = dashboardService;
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
     * Dashboard del usuario final con datos reales: proyectos, top recomendaciones y roadmap.
     */
    @PreAuthorize("hasRole('USUARIO')")
    @GetMapping("/usuario/dashboard")
    public String userDashboard(Authentication authentication, Model model) {
        Usuario usuario = resolverUsuario(authentication);
        model.addAttribute("usuario", usuario);
        model.addAttribute("topRecomendaciones",
                dashboardService.obtenerTopRecomendacionesPorProyecto(usuario.getId(), 3));
        model.addAttribute("roadmap",
                dashboardService.obtenerRoadmap(usuario.getId()));
        model.addAttribute("totalRecomendaciones",
                dashboardService.contarTotalRecomendaciones(usuario.getId()));
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
     * Resuelve el {@link Usuario} desde el contexto de autenticación.
     */
    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + authentication.getName()));
    }
}
