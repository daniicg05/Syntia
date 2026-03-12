package com.syntia.mvp.controller;

import com.syntia.mvp.model.Proyecto;
import com.syntia.mvp.model.Usuario;
import com.syntia.mvp.model.dto.ProyectoDTO;
import com.syntia.mvp.service.ProyectoService;
import com.syntia.mvp.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controlador MVC para la gestión de proyectos del usuario autenticado.
 * <p>
 * Decisión arquitectónica: se sigue el mismo patrón que {@link PerfilController},
 * resolviendo el usuario desde el contexto de autenticación en cada petición.
 * La verificación de propiedad del proyecto se delega al servicio.
 * Se usa POST para eliminar (en lugar de DELETE) porque Thymeleaf solo
 * genera formularios con GET y POST en HTML.
 */
@Controller
@RequestMapping("/usuario/proyectos")
public class ProyectoController {

    private final ProyectoService proyectoService;
    private final UsuarioService usuarioService;

    public ProyectoController(ProyectoService proyectoService, UsuarioService usuarioService) {
        this.proyectoService = proyectoService;
        this.usuarioService = usuarioService;
    }

    /**
     * Lista todos los proyectos del usuario autenticado.
     */
    @GetMapping
    public String listar(Authentication authentication, Model model) {
        Usuario usuario = resolverUsuario(authentication);
        List<Proyecto> proyectos = proyectoService.obtenerProyectos(usuario.getId());
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("usuario", usuario);
        return "usuario/proyectos/lista";
    }

    /**
     * Muestra el formulario para crear un nuevo proyecto.
     */
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevo(Authentication authentication, Model model) {
        model.addAttribute("proyectoDTO", new ProyectoDTO());
        model.addAttribute("usuario", resolverUsuario(authentication));
        return "usuario/proyectos/formulario";
    }

    /**
     * Procesa la creación de un nuevo proyecto.
     */
    @PostMapping("/nuevo")
    public String crear(@Valid @ModelAttribute("proyectoDTO") ProyectoDTO dto,
                        BindingResult result,
                        Authentication authentication,
                        Model model,
                        RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("usuario", resolverUsuario(authentication));
            return "usuario/proyectos/formulario";
        }

        Usuario usuario = resolverUsuario(authentication);
        proyectoService.crear(usuario, dto);
        redirectAttributes.addFlashAttribute("exito", "Proyecto creado correctamente.");
        return "redirect:/usuario/proyectos";
    }

    /**
     * Muestra el detalle de un proyecto.
     */
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id,
                          Authentication authentication,
                          Model model) {
        Usuario usuario = resolverUsuario(authentication);
        Proyecto proyecto = proyectoService.obtenerPorId(id, usuario.getId());
        model.addAttribute("proyecto", proyecto);
        model.addAttribute("usuario", usuario);
        return "usuario/proyectos/detalle";
    }

    /**
     * Muestra el formulario de edición de un proyecto existente.
     */
    @GetMapping("/{id}/editar")
    public String mostrarFormularioEditar(@PathVariable Long id,
                                          Authentication authentication,
                                          Model model) {
        Usuario usuario = resolverUsuario(authentication);
        Proyecto proyecto = proyectoService.obtenerPorId(id, usuario.getId());
        model.addAttribute("proyectoDTO", proyectoService.toDTO(proyecto));
        model.addAttribute("proyectoId", id);
        model.addAttribute("usuario", usuario);
        return "usuario/proyectos/formulario";
    }

    /**
     * Procesa la edición de un proyecto existente.
     */
    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Long id,
                         @Valid @ModelAttribute("proyectoDTO") ProyectoDTO dto,
                         BindingResult result,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("proyectoId", id);
            model.addAttribute("usuario", resolverUsuario(authentication));
            return "usuario/proyectos/formulario";
        }

        Usuario usuario = resolverUsuario(authentication);
        proyectoService.actualizar(id, usuario.getId(), dto);
        redirectAttributes.addFlashAttribute("exito", "Proyecto actualizado correctamente.");
        return "redirect:/usuario/proyectos/" + id;
    }

    /**
     * Elimina un proyecto. Se usa POST desde un formulario HTML.
     */
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        Usuario usuario = resolverUsuario(authentication);
        proyectoService.eliminar(id, usuario.getId());
        redirectAttributes.addFlashAttribute("exito", "Proyecto eliminado correctamente.");
        return "redirect:/usuario/proyectos";
    }

    /**
     * Resuelve el {@link Usuario} desde el contexto de autenticación.
     * Mismo patrón que en {@link PerfilController} para mantener coherencia.
     */
    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + authentication.getName()));
    }
}

