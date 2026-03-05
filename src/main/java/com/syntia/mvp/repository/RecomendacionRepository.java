package com.syntia.mvp.repository;

import com.syntia.mvp.model.Recomendacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Recomendacion.
 */
@Repository
public interface RecomendacionRepository extends JpaRepository<Recomendacion, Long> {

    /**
     * Obtiene todas las recomendaciones de un proyecto ordenadas por puntuación descendente.
     */
    List<Recomendacion> findByProyectoIdOrderByPuntuacionDesc(Long proyectoId);

    /**
     * Elimina todas las recomendaciones de un proyecto (para regenerarlas).
     */
    @Modifying
    @Query("DELETE FROM Recomendacion r WHERE r.proyecto.id = :proyectoId")
    void deleteByProyectoId(@Param("proyectoId") Long proyectoId);

    /**
     * Cuenta las recomendaciones de un proyecto.
     */
    long countByProyectoId(Long proyectoId);
}

