package com.syntia.mvp.controller;

import com.syntia.mvp.model.Proyecto;
import com.syntia.mvp.model.Recomendacion;
import com.syntia.mvp.model.Usuario;
import com.syntia.mvp.model.dto.GuiaSubvencionDTO;
import com.syntia.mvp.model.dto.RecomendacionDTO;
import com.syntia.mvp.service.BdnsClientService;
import com.syntia.mvp.service.MotorMatchingService;
import com.syntia.mvp.service.OpenAiGuiaService;
import com.syntia.mvp.service.PerfilService;
import com.syntia.mvp.service.ProyectoService;
import com.syntia.mvp.service.RecomendacionService;
import com.syntia.mvp.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    private final OpenAiGuiaService openAiGuiaService;
    private final PerfilService perfilService;
    private final BdnsClientService bdnsClientService;

    public RecomendacionController(RecomendacionService recomendacionService,
                                   MotorMatchingService motorMatchingService,
                                   ProyectoService proyectoService,
                                   UsuarioService usuarioService,
                                   OpenAiGuiaService openAiGuiaService,
                                   PerfilService perfilService,
                                   BdnsClientService bdnsClientService) {
        this.recomendacionService = recomendacionService;
        this.motorMatchingService = motorMatchingService;
        this.proyectoService = proyectoService;
        this.usuarioService = usuarioService;
        this.openAiGuiaService = openAiGuiaService;
        this.perfilService = perfilService;
        this.bdnsClientService = bdnsClientService;
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

    /**
     * Endpoint SSE para generar recomendaciones con feedback en tiempo real.
     * El análisis se ejecuta en un hilo separado, emitiendo eventos progresivos:
     * - "estado": mensajes de texto de progreso
     * - "keywords": keywords generadas por IA
     * - "busqueda": candidatas encontradas en BDNS
     * - "progreso": progreso de evaluación (actual/total)
     * - "resultado": cada recomendación encontrada (aparece en tiempo real)
     * - "completado": resumen final
     * - "error": si ocurre un error
     */
    @GetMapping(value = "/generar-stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generarStream(@PathVariable Long proyectoId,
                                     Authentication authentication) {

        // Timeout de 3 minutos — el análisis de 15 candidatas tarda ~30-60s
        SseEmitter emitter = new SseEmitter(180_000L);

        Usuario usuario = resolverUsuario(authentication);
        Proyecto proyecto = proyectoService.obtenerPorId(proyectoId, usuario.getId());

        // Ejecutar en hilo separado para no bloquear Tomcat
        CompletableFuture.runAsync(() -> {
            try {
                motorMatchingService.generarRecomendacionesStream(proyecto, emitter);
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("\"Error interno: " + e.getMessage().replace("\"", "'") + "\"",
                                    org.springframework.http.MediaType.APPLICATION_JSON));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> {
            // Silenciar — el cliente se desconectó
        });

        return emitter;
    }

    /**
     * Genera (o devuelve si ya existe) la guía enriquecida de solicitud para una recomendación.
     * <p>
     * La guía se genera bajo demanda con OpenAI usando un system prompt especializado
     * en procedimientos administrativos de subvenciones (LGS 38/2003, Ley 39/2015).
     * El resultado se persiste en BD para evitar regeneraciones innecesarias.
     *
     * @param proyectoId      ID del proyecto
     * @param recomendacionId ID de la recomendación
     * @return JSON con la guía estructurada completa (GuiaSubvencionDTO)
     */
    @GetMapping(value = "/{recomendacionId}/guia-enriquecida", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> obtenerGuiaEnriquecida(@PathVariable Long proyectoId,
                                                     @PathVariable Long recomendacionId,
                                                     Authentication authentication) {
        Usuario usuario = resolverUsuario(authentication);
        Proyecto proyecto = proyectoService.obtenerPorId(proyectoId, usuario.getId());

        // Buscar la recomendación
        Recomendacion rec = recomendacionService.obtenerEntidadPorId(recomendacionId, proyectoId);

        // Si ya tiene guía enriquecida, devolverla directamente
        if (rec.getGuiaEnriquecida() != null && !rec.getGuiaEnriquecida().isBlank()) {
            GuiaSubvencionDTO guia = openAiGuiaService.deserializarGuia(rec.getGuiaEnriquecida());
            if (guia != null) {
                return ResponseEntity.ok(guia);
            }
        }

        // Generar guía enriquecida bajo demanda
        try {
            com.syntia.mvp.model.Perfil perfil = perfilService.obtenerPerfil(usuario.getId()).orElse(null);
            com.syntia.mvp.model.Convocatoria convocatoria = rec.getConvocatoria();

            // Obtener detalle BDNS si está disponible
            String detalleTexto = convocatoria.getIdBdns() != null
                    ? bdnsClientService.obtenerDetalleTexto(convocatoria.getIdBdns())
                    : null;

            // URL oficial
            String urlOficial = convocatoria.getNumeroConvocatoria() != null
                    ? "https://www.infosubvenciones.es/bdnstrans/GE/es/convocatoria/" + convocatoria.getNumeroConvocatoria()
                    : convocatoria.getUrlOficial();

            GuiaSubvencionDTO guia = openAiGuiaService.generarGuia(
                    proyecto, perfil, convocatoria, detalleTexto, urlOficial);

            // Persistir para evitar regeneraciones
            String guiaJson = openAiGuiaService.serializarGuia(guia);
            recomendacionService.actualizarGuiaEnriquecida(recomendacionId, guiaJson);

            return ResponseEntity.ok(guia);

        } catch (com.syntia.mvp.service.OpenAiClient.OpenAiUnavailableException e) {
            return ResponseEntity.status(503).body(
                    java.util.Map.of("error", "El servicio de IA no está disponible: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    java.util.Map.of("error", "Error generando la guía: " + e.getMessage()));
        }
    }

    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + authentication.getName()));
    }
}

