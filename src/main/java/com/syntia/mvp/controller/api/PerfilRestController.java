package com.syntia.mvp.controller.api;

import com.syntia.mvp.model.Perfil;
import com.syntia.mvp.model.Usuario;
import com.syntia.mvp.model.dto.PerfilDTO;
import com.syntia.mvp.service.PerfilService;
import com.syntia.mvp.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * Controlador REST para el perfil del usuario autenticado.
 * Rutas bajo {@code /api/usuario/perfil}, protegidas con JWT.
 */
@RestController
@RequestMapping("/api/usuario/perfil")
public class PerfilRestController {

    private final PerfilService perfilService;
    private final UsuarioService usuarioService;

    public PerfilRestController(PerfilService perfilService, UsuarioService usuarioService) {
        this.perfilService = perfilService;
        this.usuarioService = usuarioService;
    }

    /**
     * Devuelve el perfil del usuario autenticado.
     *
     * @return 200 + PerfilDTO, o 404 si aún no ha creado perfil
     */
    @GetMapping
    public ResponseEntity<?> obtenerPerfil(Authentication authentication) {
        Usuario usuario = resolverUsuario(authentication);
        Optional<Perfil> perfil = perfilService.obtenerPerfil(usuario.getId());
        if (perfil.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of("mensaje", "El usuario aún no ha completado su perfil"));
        }
        return ResponseEntity.ok(perfilService.toDTO(perfil.get()));
    }

    /**
     * Crea o actualiza el perfil del usuario autenticado.
     *
     * @param dto datos del perfil
     * @return 200 + mensaje de confirmación
     */
    @PutMapping
    public ResponseEntity<?> guardarPerfil(@Valid @RequestBody PerfilDTO dto,
                                           Authentication authentication) {
        Usuario usuario = resolverUsuario(authentication);
        if (perfilService.tienePerfil(usuario.getId())) {
            perfilService.actualizarPerfil(usuario.getId(), dto);
            return ResponseEntity.ok(Map.of("mensaje", "Perfil actualizado correctamente"));
        } else {
            perfilService.crearPerfil(usuario, dto);
            return ResponseEntity.ok(Map.of("mensaje", "Perfil creado correctamente"));
        }
    }

    private Usuario resolverUsuario(Authentication authentication) {
        return usuarioService.buscarPorEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + authentication.getName()));
    }
}

