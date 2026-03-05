package com.syntia.mvp.repository;

import com.syntia.mvp.model.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Proyecto.
 */
@Repository
public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {

    /**
     * Obtiene todos los proyectos de un usuario.
     * @param usuarioId ID del usuario
     * @return lista de proyectos
     */
    List<Proyecto> findByUsuarioId(Long usuarioId);
}

