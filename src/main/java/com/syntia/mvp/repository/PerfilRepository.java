package com.syntia.mvp.repository;

import com.syntia.mvp.model.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad Perfil.
 */
@Repository
public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    /**
     * Busca el perfil asociado a un usuario.
     * @param usuarioId ID del usuario
     * @return Optional con el perfil si existe
     */
    Optional<Perfil> findByUsuarioId(Long usuarioId);
}

