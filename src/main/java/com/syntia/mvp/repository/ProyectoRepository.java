package com.syntia.mvp.repository;

import com.syntia.mvp.model.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    /**
     * Cuenta el total de proyectos del sistema en una sola query.
     * Evita el patron N+1 que itere sobre usuarios.
     */
    @Query("SELECT COUNT(p) FROM Proyecto p")
    long countAll();
}

