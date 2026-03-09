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
     * Usa JOIN FETCH para cargar la convocatoria en la misma query y evitar
     * LazyInitializationException al convertir a DTO.
     */
    @Query("SELECT r FROM Recomendacion r JOIN FETCH r.convocatoria WHERE r.proyecto.id = :proyectoId ORDER BY r.puntuacion DESC")
    List<Recomendacion> findByProyectoIdOrderByPuntuacionDesc(@Param("proyectoId") Long proyectoId);

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

    /**
     * Cuenta el total de recomendaciones del sistema en una sola query.
     * Evita el patron N+1 que itere sobre proyectos.
     */
    @Query("SELECT COUNT(r) FROM Recomendacion r")
    long countAll();

    /**
     * Filtra recomendaciones de un proyecto por tipo, sector y ubicacion delegando a BD.
     * Usa JOIN FETCH para cargar la convocatoria y evitar LazyInitializationException.
     * Los parametros nulos o vacios se ignoran (filtro opcional).
     */
    @Query("SELECT r FROM Recomendacion r JOIN FETCH r.convocatoria c " +
           "WHERE r.proyecto.id = :proyectoId " +
           "AND (:tipo      IS NULL OR :tipo      = '' OR c.tipo      = :tipo) " +
           "AND (:sector    IS NULL OR :sector    = '' OR c.sector    = :sector) " +
           "AND (:ubicacion IS NULL OR :ubicacion = '' OR c.ubicacion = :ubicacion) " +
           "ORDER BY r.puntuacion DESC")
    List<Recomendacion> filtrar(@Param("proyectoId") Long proyectoId,
                                @Param("tipo")       String tipo,
                                @Param("sector")     String sector,
                                @Param("ubicacion")  String ubicacion);

    /**
     * Obtiene los tipos de convocatoria distintos para las recomendaciones de un proyecto.
     * Usado para poblar el selector de filtros en la vista.
     */
    @Query("SELECT DISTINCT c.tipo FROM Recomendacion r JOIN r.convocatoria c " +
           "WHERE r.proyecto.id = :proyectoId AND c.tipo IS NOT NULL ORDER BY c.tipo")
    List<String> findTiposDistintosByProyectoId(@Param("proyectoId") Long proyectoId);

    /**
     * Obtiene los sectores distintos para las recomendaciones de un proyecto.
     * Usado para poblar el selector de filtros en la vista.
     */
    @Query("SELECT DISTINCT c.sector FROM Recomendacion r JOIN r.convocatoria c " +
           "WHERE r.proyecto.id = :proyectoId AND c.sector IS NOT NULL ORDER BY c.sector")
    List<String> findSectoresDistintosByProyectoId(@Param("proyectoId") Long proyectoId);
}

