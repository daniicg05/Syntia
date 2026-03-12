package com.syntia.mvp.repository;

import com.syntia.mvp.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad Usuario.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su email.
     * @param email email del usuario
     * @return Optional con el usuario si existe
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Comprueba si existe un usuario con el email dado.
     * @param email email a comprobar
     * @return true si existe
     */
    boolean existsByEmail(String email);
}

