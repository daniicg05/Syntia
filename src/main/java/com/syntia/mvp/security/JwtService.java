package com.syntia.mvp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Servicio para la generación, validación y extracción de información de tokens JWT.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Genera un token JWT para un usuario autenticado.
     *
     * @param email email del usuario (subject del token)
     * @param rol   rol del usuario
     * @return token JWT firmado
     */
    public String generarToken(String email, String rol) {
        return Jwts.builder()
                .subject(email)
                .claims(Map.of("rol", rol))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrae el email (subject) del token.
     *
     * @param token token JWT
     * @return email del usuario
     */
    public String extraerUsername(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    /**
     * Extrae el rol del token.
     *
     * @param token token JWT
     * @return rol del usuario
     */
    public String extraerRol(String token) {
        return extraerClaim(token, claims -> claims.get("rol", String.class));
    }

    /**
     * Valida si el token es válido (firma correcta y no expirado).
     *
     * @param token    token JWT
     * @param username email del usuario a verificar
     * @return true si el token es válido
     */
    public boolean validarToken(String token, String username) {
        final String tokenUsername = extraerUsername(token);
        return tokenUsername.equals(username) && !isTokenExpirado(token);
    }

    /**
     * Extrae un claim específico del token.
     */
    private <T> T extraerClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extraerTodosClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrae todos los claims del token.
     */
    private Claims extraerTodosClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Comprueba si el token ha expirado.
     */
    private boolean isTokenExpirado(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Obtiene la clave de firma a partir del secret configurado.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

