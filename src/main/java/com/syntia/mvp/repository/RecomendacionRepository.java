package com.syntia.mvp.repository;

import com.syntia.mvp.model.Recomendacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Recomendacion.
 */
@Repository
public interface RecomendacionRepository extends JpaRepository<Recomendacion, Long> {

    /**
     * Obtiene todas las recomendaciones de un proyecto, ordenadas por puntuación descendente.
     * @param proyectoId ID del proyecto
     * @return lista de recomendaciones ordenadas
     */
    List<Recomendacion> findByProyectoIdOrderByPuntuacionDesc(Long proyectoId);
}

