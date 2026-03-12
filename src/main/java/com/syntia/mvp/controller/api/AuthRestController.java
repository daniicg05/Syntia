package com.syntia.mvp.controller.api;

import com.syntia.mvp.model.Usuario;
import com.syntia.mvp.model.dto.LoginRequestDTO;
import com.syntia.mvp.model.dto.LoginResponseDTO;
import com.syntia.mvp.security.JwtService;
import com.syntia.mvp.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para autenticación.
 * Expone {@code POST /api/auth/login} que valida credenciales y devuelve un JWT.
 * <p>
 * Decisión arquitectónica: usa {@link AuthenticationManager} de Spring Security
 * para validar credenciales, delegando en {@link com.syntia.mvp.service.CustomUserDetailsService}.
 * Así se reutiliza la misma lógica de autenticación que el login por formulario.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public AuthRestController(AuthenticationManager authenticationManager,
                              JwtService jwtService,
                              UsuarioService usuarioService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
    }

    /**
     * Autentica un usuario y devuelve un token JWT.
     *
     * @param request DTO con email y password
     * @return 200 + {@link LoginResponseDTO} con el token, o 401 si las credenciales son incorrectas
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("error", "Credenciales incorrectas"));
        }

        Usuario usuario = usuarioService.buscarPorEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado tras autenticación"));

        String token = jwtService.generarToken(usuario.getEmail(), usuario.getRol().name());

        return ResponseEntity.ok(new LoginResponseDTO(
                token,
                usuario.getEmail(),
                usuario.getRol().name(),
                jwtExpiration
        ));
    }
}

