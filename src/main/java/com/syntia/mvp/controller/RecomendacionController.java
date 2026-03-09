package com.syntia.mvp.controller;

import com.syntia.mvp.model.Proyecto;
import com.syntia.mvp.model.Usuario;
import com.syntia.mvp.model.dto.RecomendacionDTO;
import com.syntia.mvp.service.MotorMatchingService;
import com.syntia.mvp.service.ProyectoService;
import com.syntia.mvp.service.RecomendacionService;
import com.syntia.mvp.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controlador MVC para las recomendaciones de un proyecto.
 * <p>
 * Decisión arquitectónica: las rutas de recomendaciones se ubican bajo
 * {@code /usuario/proyectos/{id}/recomendaciones} para mantener la jerarquía
 * lógica proyecto → recomendaciones, coherente con el modelo de datos.
 */
@Controller
@RequestMapping("/usuario/proyectos/{proyectoId}/recomendaciones")
public class RecomendacionController {

    private final RecomendacionService recomendacionService;
    private final MotorMatchingService motorMatchingService;
    private final ProyectoService proyectoService;
    private final UsuarioService usuarioService;

    public RecomendacionController(RecomendacionService recomendacionService,
                                   MotorMatchingService motorMatchingService,
                                   ProyectoService proyectoService,
                                   UsuarioService usuarioService) {
        this.recomendacionService = recomendacionService;
        this.motorMatchingService = motorMatchingService;
        this.proyectoService = proyectoService;
        this.usuarioService = usuarioService;
    }

    /**
     * Muestra las recomendaciones de un proyecto con filtros opcionales.
     * Si no hay recomendaciones generadas aún, muestra mensaje de bienvenida.
     *
     * @param proyectoId ID del proyecto
     * @param tipo       filtro opcional por tipo de convocatoria
     * @param sector     filtro opcional por sector
     * @param ubicacion  filtro opcional por ubicación
     */
    @GetMapping
    public String verRecomendaciones(@PathVariable Long proyectoId,
                                     @RequestParam(required = false) String tipo,
                                     @RequestParam(required = false) String sector,
                                     @RequestParam(required = false) String ubicacion,
                                     Authentication authentication,
                                     Model model) {

        Usuario usuario = resolverUsuario(authentication);
        Proyecto proyecto = proyectoService.obtenerPorId(proyectoId, usuario.getId());

        // Filtros delegados a BD — una sola query en lugar de cargar todo + filtrar en memoria
        List<RecomendacionDTO> recomendaciones = recomendacionService.filtrar(proyectoId, tipo, sector, ubicacion);

        // Selectores de filtro: valores distintos via queries BD (no iterar sobre todos los registros)
        List<String> tipos    = recomendacionService.obtenerTiposDistintos(proyectoId);
        List<String> sectores = recomendacionService.obtenerSectoresDistintos(proyectoId);

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("recomendaciones", recomendaciones);
        model.addAttribute("totalSinFiltro", recomendacionService.contarPorProyecto(proyectoId));
        model.addAttribute("tipos", tipos);
        model.addAttribute("sectores", sectores);
        model.addAttribute("filtrTipo", tipo);
        model.addAttribute("filtrSector", sector);
        model.addAttribute("filtrUbicacion", ubicacion);
        model.addAttribute("usuario", usuario);
        return "usuario/proyectos/recomendaciones";
    }

    /**
     * Dispara el motor de matching para regenerar las recomendaciones del proyecto.
     * Redirige a la vista de recomendaciones con mensaje de resultado.
     */
    @PostMapping("/generar")
    public String generarRecomendaciones(@PathVariable Long proyectoId,
                                         Authentication authentication,
                                         RedirectAttributes redirectAttributes) {

        Usuario usuario = resolverUsuario(authentication);
        Proyecto proyecto = proyectoService.obtenerPorId(proyectoId, usuario.getId());

        List<?> generadas;
        try {
            generadas = motorMatchingService.generarRecomendaciones(proyecto);
        } catch (com.syntia.mvp.service.OpenAiClient.OpenAiUnavailableException e) {
            redirectAttributes.addFlashAttribute("aviso",
                    "El servicio de IA no está disponible en este momento. " +
                    "Verifica que la clave de OpenAI esté configurada correctamente. " +
                    "Detalle: " + e.getMessage());
            return "redirect:/usuario/proyectos/" + proyectoId + "/recomendaciones";
        } catch (com.syntia.mvp.service.BdnsClientService.BdnsException e) {
            redirectAttributes.addFlashAttribute("aviso",
                    "No se pudo conectar con la Base de Datos Nacional de Subvenciones (BDNS). " +
                    "Inténtalo de nuevo más tarde.");
            return "redirect:/usuario/proyectos/" + proyectoId + "/recomendaciones";
        }

        // Contar desde BD para que el mensaje coincida exactamente con lo que mostrará la vista
        long totalEnBd = recomendacionService.contarPorProyecto(proyectoId);

        if (totalEnBd == 0) {
            redirectAttributes.addFlashAttribute("aviso",
                    "El motor de IA no encontró convocatorias compatibles con tu proyecto. " +
                    "Completa el sector, la ubicación y la descripción para mejorar los resultados.");
        } else {
            long conIa = generadas.stream()
                    .filter(r -> r instanceof com.syntia.mvp.model.Recomendacion rec && rec.isUsadaIa())
                    .count();
            String msg = "Se han encontrado " + totalEnBd + " recomendaciones para tu proyecto.";
            if (conIa > 0) msg += " " + conIa + " analizadas por IA.";
            redirectAttributes.addFlashAttribute("exito", msg);
        }
        return "redirect:/usuario/proyectos/" + proyectoId + "/recomendaciones";
    }

    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + authentication.getName()));
    }
}

