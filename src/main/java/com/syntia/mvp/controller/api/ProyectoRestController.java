package com.syntia.mvp.controller.api;

import com.syntia.mvp.model.Proyecto;
import com.syntia.mvp.model.Usuario;
import com.syntia.mvp.model.dto.ProyectoDTO;
import com.syntia.mvp.service.ProyectoService;
import com.syntia.mvp.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de proyectos del usuario autenticado.
 * Rutas bajo {@code /api/usuario/proyectos}, protegidas con JWT.
 * <p>
 * Decisión de seguridad: todas las respuestas usan {@link ProyectoDTO} en lugar
 * de la entidad JPA {@link Proyecto} para evitar la exposición accidental de la
 * relación {@code @ManyToOne Usuario} y sus datos sensibles (password_hash, etc.).
 */
@RestController
@RequestMapping("/api/usuario/proyectos")
public class ProyectoRestController {

    private final ProyectoService proyectoService;
    private final UsuarioService usuarioService;

    public ProyectoRestController(ProyectoService proyectoService, UsuarioService usuarioService) {
        this.proyectoService = proyectoService;
        this.usuarioService = usuarioService;
    }

    /**
     * Lista todos los proyectos del usuario autenticado como DTOs.
     */
    @GetMapping
    public ResponseEntity<List<ProyectoDTO>> listar(Authentication authentication) {
        Usuario usuario = resolverUsuario(authentication);
        List<ProyectoDTO> dtos = proyectoService.obtenerProyectos(usuario.getId())
                .stream()
                .map(proyectoService::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Obtiene un proyecto por ID como DTO (solo si pertenece al usuario autenticado).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProyectoDTO> obtener(@PathVariable Long id,
                                               Authentication authentication) {
        Usuario usuario = resolverUsuario(authentication);
        return ResponseEntity.ok(
                proyectoService.toDTO(proyectoService.obtenerPorId(id, usuario.getId())));
    }

    /**
     * Crea un nuevo proyecto y devuelve el DTO resultante.
     */
    @PostMapping
    public ResponseEntity<ProyectoDTO> crear(@Valid @RequestBody ProyectoDTO dto,
                                             Authentication authentication) {
        Usuario usuario = resolverUsuario(authentication);
        return ResponseEntity.ok(
                proyectoService.toDTO(proyectoService.crear(usuario, dto)));
    }

    /**
     * Actualiza un proyecto existente y devuelve el DTO actualizado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProyectoDTO> actualizar(@PathVariable Long id,
                                                  @Valid @RequestBody ProyectoDTO dto,
                                                  Authentication authentication) {
        Usuario usuario = resolverUsuario(authentication);
        return ResponseEntity.ok(
                proyectoService.toDTO(proyectoService.actualizar(id, usuario.getId(), dto)));
    }

    /**
     * Elimina un proyecto del usuario autenticado.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id, Authentication authentication) {
        Usuario usuario = resolverUsuario(authentication);
        proyectoService.eliminar(id, usuario.getId());
        return ResponseEntity.ok(Map.of("mensaje", "Proyecto eliminado correctamente"));
    }

    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + authentication.getName()));
    }
}


