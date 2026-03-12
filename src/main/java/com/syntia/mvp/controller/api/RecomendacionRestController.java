package com.syntia.mvp.controller.api;

import com.syntia.mvp.model.Proyecto;
import com.syntia.mvp.model.Recomendacion;
import com.syntia.mvp.model.Usuario;
import com.syntia.mvp.model.dto.GuiaSubvencionDTO;
import com.syntia.mvp.model.dto.RecomendacionDTO;
import com.syntia.mvp.service.BdnsClientService;
import com.syntia.mvp.service.MotorMatchingService;
import com.syntia.mvp.service.OpenAiClient;
import com.syntia.mvp.service.OpenAiGuiaService;
import com.syntia.mvp.service.PerfilService;
import com.syntia.mvp.service.ProyectoService;
import com.syntia.mvp.service.RecomendacionService;
import com.syntia.mvp.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para las recomendaciones de un proyecto.
 * Rutas bajo {@code /api/usuario/proyectos/{id}/recomendaciones}, protegidas con JWT.
 */
@RestController
@RequestMapping("/api/usuario/proyectos/{proyectoId}/recomendaciones")
public class RecomendacionRestController {

    private final RecomendacionService recomendacionService;
    private final MotorMatchingService motorMatchingService;
    private final ProyectoService proyectoService;
    private final UsuarioService usuarioService;
    private final OpenAiGuiaService openAiGuiaService;
    private final PerfilService perfilService;
    private final BdnsClientService bdnsClientService;

    public RecomendacionRestController(RecomendacionService recomendacionService,
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
     * Devuelve las recomendaciones de un proyecto, ordenadas por puntuación desc.
     *
     * @return 200 + lista de RecomendacionDTO
     */
    @GetMapping
    public ResponseEntity<List<RecomendacionDTO>> obtener(@PathVariable Long proyectoId,
                                                          Authentication authentication) {
        Usuario usuario = resolverUsuario(authentication);
        // Verificar propiedad del proyecto
        proyectoService.obtenerPorId(proyectoId, usuario.getId());
        return ResponseEntity.ok(recomendacionService.obtenerPorProyecto(proyectoId));
    }

    /**
     * Dispara el motor de matching y regenera las recomendaciones del proyecto.
     *
     * @return 200 + número de recomendaciones generadas
     */
    @PostMapping("/generar")
    public ResponseEntity<?> generar(@PathVariable Long proyectoId,
                                     Authentication authentication) {
        Usuario usuario = resolverUsuario(authentication);
        Proyecto proyecto = proyectoService.obtenerPorId(proyectoId, usuario.getId());
        int total = motorMatchingService.generarRecomendaciones(proyecto).size();
        return ResponseEntity.ok(Map.of(
                "mensaje", "Recomendaciones generadas correctamente",
                "total", total
        ));
    }

    /**
     * Genera (o devuelve si ya existe) la guía enriquecida de solicitud para una recomendación.
     * Endpoint REST equivalente al MVC — para consumo desde API con JWT.
     *
     * @param proyectoId      ID del proyecto
     * @param recomendacionId ID de la recomendación
     * @return JSON con la guía estructurada completa (GuiaSubvencionDTO)
     */
    @GetMapping("/{recomendacionId}/guia-enriquecida")
    public ResponseEntity<?> obtenerGuiaEnriquecida(@PathVariable Long proyectoId,
                                                     @PathVariable Long recomendacionId,
                                                     Authentication authentication) {
        Usuario usuario = resolverUsuario(authentication);
        Proyecto proyecto = proyectoService.obtenerPorId(proyectoId, usuario.getId());

        Recomendacion rec = recomendacionService.obtenerEntidadPorId(recomendacionId, proyectoId);

        // Devolver guía cacheada si existe
        if (rec.getGuiaEnriquecida() != null && !rec.getGuiaEnriquecida().isBlank()) {
            GuiaSubvencionDTO guia = openAiGuiaService.deserializarGuia(rec.getGuiaEnriquecida());
            if (guia != null) {
                return ResponseEntity.ok(guia);
            }
        }

        // Generar bajo demanda
        try {
            var perfil = perfilService.obtenerPerfil(usuario.getId()).orElse(null);
            var convocatoria = rec.getConvocatoria();

            String detalleTexto = convocatoria.getIdBdns() != null
                    ? bdnsClientService.obtenerDetalleTexto(convocatoria.getIdBdns())
                    : null;

            String urlOficial = convocatoria.getNumeroConvocatoria() != null
                    ? "https://www.infosubvenciones.es/bdnstrans/GE/es/convocatoria/" + convocatoria.getNumeroConvocatoria()
                    : convocatoria.getUrlOficial();

            GuiaSubvencionDTO guia = openAiGuiaService.generarGuia(
                    proyecto, perfil, convocatoria, detalleTexto, urlOficial);

            String guiaJson = openAiGuiaService.serializarGuia(guia);
            recomendacionService.actualizarGuiaEnriquecida(recomendacionId, guiaJson);

            return ResponseEntity.ok(guia);

        } catch (OpenAiClient.OpenAiUnavailableException e) {
            return ResponseEntity.status(503).body(
                    Map.of("error", "El servicio de IA no está disponible: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Error generando la guía: " + e.getMessage()));
        }
    }

    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + authentication.getName()));
    }
}

