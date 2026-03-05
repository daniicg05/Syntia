package com.syntia.mvp.controller.api;

import com.syntia.mvp.model.Proyecto;
import com.syntia.mvp.model.Usuario;
import com.syntia.mvp.model.dto.RecomendacionDTO;
import com.syntia.mvp.service.MotorMatchingService;
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

    public RecomendacionRestController(RecomendacionService recomendacionService,
                                       MotorMatchingService motorMatchingService,
                                       ProyectoService proyectoService,
                                       UsuarioService usuarioService) {
        this.recomendacionService = recomendacionService;
        this.motorMatchingService = motorMatchingService;
        this.proyectoService = proyectoService;
        this.usuarioService = usuarioService;
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

    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + authentication.getName()));
    }
}

