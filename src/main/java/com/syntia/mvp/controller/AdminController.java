package com.syntia.mvp.controller;

import com.syntia.mvp.model.Proyecto;
import com.syntia.mvp.model.Rol;
import com.syntia.mvp.model.Usuario;
import com.syntia.mvp.model.dto.ConvocatoriaDTO;
import com.syntia.mvp.repository.ProyectoRepository;
import com.syntia.mvp.repository.RecomendacionRepository;
import com.syntia.mvp.service.ConvocatoriaService;
import com.syntia.mvp.service.ProyectoService;
import com.syntia.mvp.service.RecomendacionService;
import com.syntia.mvp.service.UsuarioService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador del panel administrativo de Syntia.
 * <p>
 * Todas las rutas están bajo {@code /admin/**} y protegidas con {@code ROLE_ADMIN}.
 * Gestiona: usuarios, convocatorias y métricas del sistema.
 * <p>
 * Decisión arquitectónica: se centraliza toda la lógica admin en un único
 * controller con secciones bien diferenciadas (usuarios / convocatorias / métricas)
 * para mantener la navegación simple en el MVP.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UsuarioService usuarioService;
    private final ConvocatoriaService convocatoriaService;
    private final ProyectoService proyectoService;
    private final RecomendacionService recomendacionService;
    private final ProyectoRepository proyectoRepository;
    private final RecomendacionRepository recomendacionRepository;

    public AdminController(UsuarioService usuarioService,
                           ConvocatoriaService convocatoriaService,
                           ProyectoService proyectoService,
                           RecomendacionService recomendacionService,
                           ProyectoRepository proyectoRepository,
                           RecomendacionRepository recomendacionRepository) {
        this.usuarioService = usuarioService;
        this.convocatoriaService = convocatoriaService;
        this.proyectoService = proyectoService;
        this.recomendacionService = recomendacionService;
        this.proyectoRepository = proyectoRepository;
        this.recomendacionRepository = recomendacionRepository;
    }

    // ─────────────────────────────────────────────
    // DASHBOARD ADMIN
    // ─────────────────────────────────────────────

    /**
     * Dashboard del administrador con métricas básicas del sistema.
     * Usa countAll() directo para evitar el patron N+1.
     */
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("adminEmail", authentication.getName());
        model.addAttribute("totalUsuarios",       usuarioService.obtenerTodos().size());
        model.addAttribute("totalConvocatorias",  convocatoriaService.obtenerTodas().size());
        // Queries directas en BD — sin iterar sobre colecciones en memoria
        model.addAttribute("totalProyectos",       proyectoRepository.countAll());
        model.addAttribute("totalRecomendaciones", recomendacionRepository.countAll());
        return "admin/dashboard";
    }

    // ─────────────────────────────────────────────
    // GESTIÓN DE USUARIOS
    // ─────────────────────────────────────────────

    /**
     * Lista todos los usuarios registrados.
     */
    @GetMapping("/usuarios")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.obtenerTodos());
        model.addAttribute("roles", Rol.values());
        return "admin/usuarios/lista";
    }

    /**
     * Ver detalle de un usuario: datos basicos, proyectos y recomendaciones por proyecto.
     */
    @GetMapping("/usuarios/{id}")
    public String detalleUsuario(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + id));
        List<Proyecto> proyectos = proyectoService.obtenerProyectos(id);

        // Mapa proyectoId -> nº recomendaciones para mostrarlo en la tabla sin N+1
        Map<Long, Long> recsPerProyecto = proyectos.stream()
                .collect(Collectors.toMap(
                        Proyecto::getId,
                        p -> recomendacionService.contarPorProyecto(p.getId())
                ));

        model.addAttribute("usuarioDetalle", usuario);
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("recsPerProyecto", recsPerProyecto);
        return "admin/usuarios/detalle";
    }

    /**
     * Cambia el rol de un usuario.
     */
    @PostMapping("/usuarios/{id}/rol")
    public String cambiarRol(@PathVariable Long id,
                             @RequestParam Rol rol,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        // Protección: el admin no puede cambiar su propio rol
        Usuario admin = resolverUsuario(authentication);
        if (admin.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "No puedes cambiar tu propio rol.");
            return "redirect:/admin/usuarios";
        }
        usuarioService.cambiarRol(id, rol);
        redirectAttributes.addFlashAttribute("exito", "Rol actualizado correctamente.");
        return "redirect:/admin/usuarios";
    }

    /**
     * Elimina un usuario.
     */
    @PostMapping("/usuarios/{id}/eliminar")
    public String eliminarUsuario(@PathVariable Long id,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        // Protección: el admin no puede eliminarse a sí mismo
        Usuario admin = resolverUsuario(authentication);
        if (admin.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "No puedes eliminarte a ti mismo.");
            return "redirect:/admin/usuarios";
        }
        usuarioService.eliminar(id);
        redirectAttributes.addFlashAttribute("exito", "Usuario eliminado correctamente.");
        return "redirect:/admin/usuarios";
    }

    // ─────────────────────────────────────────────
    // GESTIÓN DE CONVOCATORIAS
    // ─────────────────────────────────────────────

    /**
     * Lista todas las convocatorias.
     */
    @GetMapping("/convocatorias")
    public String listarConvocatorias(Model model) {
        model.addAttribute("convocatorias", convocatoriaService.obtenerTodas());
        return "admin/convocatorias/lista";
    }

    /**
     * Formulario para crear una nueva convocatoria.
     */
    @GetMapping("/convocatorias/nueva")
    public String formNuevaConvocatoria(Model model) {
        model.addAttribute("convocatoriaDTO", new ConvocatoriaDTO());
        return "admin/convocatorias/formulario";
    }

    /**
     * Procesa la creación de una nueva convocatoria.
     */
    @PostMapping("/convocatorias/nueva")
    public String crearConvocatoria(@Valid @ModelAttribute("convocatoriaDTO") ConvocatoriaDTO dto,
                                    BindingResult result,
                                    RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "admin/convocatorias/formulario";
        convocatoriaService.crear(dto);
        redirectAttributes.addFlashAttribute("exito", "Convocatoria creada correctamente.");
        return "redirect:/admin/convocatorias";
    }

    /**
     * Formulario para editar una convocatoria existente.
     */
    @GetMapping("/convocatorias/{id}/editar")
    public String formEditarConvocatoria(@PathVariable Long id, Model model) {
        model.addAttribute("convocatoriaDTO", convocatoriaService.toDTO(convocatoriaService.obtenerPorId(id)));
        model.addAttribute("convocatoriaId", id);
        return "admin/convocatorias/formulario";
    }

    /**
     * Procesa la edición de una convocatoria.
     */
    @PostMapping("/convocatorias/{id}/editar")
    public String editarConvocatoria(@PathVariable Long id,
                                     @Valid @ModelAttribute("convocatoriaDTO") ConvocatoriaDTO dto,
                                     BindingResult result,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("convocatoriaId", id);
            return "admin/convocatorias/formulario";
        }
        convocatoriaService.actualizar(id, dto);
        redirectAttributes.addFlashAttribute("exito", "Convocatoria actualizada correctamente.");
        return "redirect:/admin/convocatorias";
    }

    /**
     * Elimina una convocatoria.
     */
    @PostMapping("/convocatorias/{id}/eliminar")
    public String eliminarConvocatoria(@PathVariable Long id,
                                       RedirectAttributes redirectAttributes) {
        convocatoriaService.eliminar(id);
        redirectAttributes.addFlashAttribute("exito", "Convocatoria eliminada correctamente.");
        return "redirect:/admin/convocatorias";
    }

    /**
     * Importa convocatorias desde la API publica de BDNS.
     * En caso de error de conexion muestra un mensaje de aviso sin lanzar excepcion.
     */
    @PostMapping("/convocatorias/importar-bdns")
    public String importarDesdeBdns(@RequestParam(defaultValue = "0")  int pagina,
                                    @RequestParam(defaultValue = "20") int tamano,
                                    RedirectAttributes redirectAttributes) {
        try {
            int nuevas = convocatoriaService.importarDesdeBdns(pagina, tamano);
            if (nuevas == 0) {
                redirectAttributes.addFlashAttribute("aviso",
                        "Se consultó BDNS pero no se encontraron convocatorias nuevas (todas ya existen en el catálogo).");
            } else {
                redirectAttributes.addFlashAttribute("exito",
                        "Se importaron " + nuevas + " convocatorias nuevas desde BDNS.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "No se pudo conectar con la API de BDNS: " + e.getMessage() +
                    ". Activa el modo mock con bdns.mock=true para pruebas.");
        }
        return "redirect:/admin/convocatorias";
    }

    // ─────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────

    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + authentication.getName()));
    }
}

