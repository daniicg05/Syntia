package com.syntia.mvp.controller;

import com.syntia.mvp.model.Perfil;
import com.syntia.mvp.model.Usuario;
import com.syntia.mvp.model.dto.PerfilDTO;
import com.syntia.mvp.service.PerfilService;
import com.syntia.mvp.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * Controlador MVC para la gestión del perfil del usuario autenticado.
 * <p>
 * Decisión arquitectónica: una única ruta {@code /usuario/perfil} gestiona tanto
 * la creación como la edición. Si el perfil ya existe se precarga el formulario;
 * si no existe se crea uno nuevo al guardar. Así el usuario siempre accede a la
 * misma URL sin necesidad de rutas separadas para crear/editar.
 */
@Controller
@RequestMapping("/usuario/perfil")
public class PerfilController {

    private final PerfilService perfilService;
    private final UsuarioService usuarioService;

    public PerfilController(PerfilService perfilService, UsuarioService usuarioService) {
        this.perfilService = perfilService;
        this.usuarioService = usuarioService;
    }

    /**
     * Muestra el formulario de perfil.
     * Si el usuario ya tiene perfil, lo precarga para edición.
     * Si no, muestra el formulario vacío para creación.
     */
    @GetMapping
    public String mostrarPerfil(Authentication authentication, Model model) {
        Usuario usuario = resolverUsuario(authentication);
        Optional<Perfil> perfilOpt = perfilService.obtenerPerfil(usuario.getId());

        if (perfilOpt.isPresent()) {
            model.addAttribute("perfilDTO", perfilService.toDTO(perfilOpt.get()));
            model.addAttribute("tienePerfil", true);
        } else {
            model.addAttribute("perfilDTO", new PerfilDTO());
            model.addAttribute("tienePerfil", false);
        }

        model.addAttribute("usuario", usuario);
        return "usuario/perfil";
    }

    /**
     * Muestra el perfil del usuario en modo solo lectura.
     * Si el usuario no tiene perfil, redirige al formulario de creación.
     */
    @GetMapping("/ver")
    public String verPerfil(Authentication authentication, Model model) {
        Usuario usuario = resolverUsuario(authentication);
        Optional<Perfil> perfilOpt = perfilService.obtenerPerfil(usuario.getId());

        if (perfilOpt.isEmpty()) {
            return "redirect:/usuario/perfil";
        }

        model.addAttribute("perfil", perfilService.toDTO(perfilOpt.get()));
        model.addAttribute("usuario", usuario);
        return "usuario/perfil-ver";
    }

    /**
     * Procesa el guardado del perfil (creación o actualización).
     * Decide automáticamente si crear o actualizar según si ya existe perfil.
     */
    @PostMapping
    public String guardarPerfil(@Valid @ModelAttribute("perfilDTO") PerfilDTO dto,
                                BindingResult result,
                                Authentication authentication,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            Usuario usuario = resolverUsuario(authentication);
            model.addAttribute("usuario", usuario);
            model.addAttribute("tienePerfil", perfilService.tienePerfil(usuario.getId()));
            return "usuario/perfil";
        }

        Usuario usuario = resolverUsuario(authentication);

        if (perfilService.tienePerfil(usuario.getId())) {
            perfilService.actualizarPerfil(usuario.getId(), dto);
            redirectAttributes.addFlashAttribute("exito", "Perfil actualizado correctamente.");
        } else {
            perfilService.crearPerfil(usuario, dto);
            redirectAttributes.addFlashAttribute("exito", "Perfil creado correctamente.");
        }

        return "redirect:/usuario/perfil";
    }

    /**
     * Resuelve el {@link Usuario} a partir del contexto de autenticación.
     * Centraliza la obtención del usuario para no repetirlo en cada método.
     */
    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + authentication.getName()));
    }
}

