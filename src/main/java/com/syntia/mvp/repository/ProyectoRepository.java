package com.syntia.mvp.repository;

import com.syntia.mvp.model.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
     * Busca un proyecto por ID haciendo JOIN FETCH del usuario para evitar
     * LazyInitializationException al acceder a proyecto.getUsuario()
     * fuera de una transacción activa.
     */
    @Query("SELECT p FROM Proyecto p JOIN FETCH p.usuario WHERE p.id = :id")
    Optional<Proyecto> findByIdWithUsuario(@Param("id") Long id);

    /**
     * Cuenta el total de proyectos del sistema en una sola query.
     * Evita el patron N+1 que itere sobre usuarios.
     */
    @Query("SELECT COUNT(p) FROM Proyecto p")
    long countAll();
}

