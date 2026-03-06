package com.syntia.mvp.repository;

import com.syntia.mvp.model.Convocatoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Convocatoria.
 */
@Repository
public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Long> {

    List<Convocatoria> findBySector(String sector);

    List<Convocatoria> findByUbicacion(String ubicacion);

    List<Convocatoria> findByTipo(String tipo);

    /**
     * Verifica si ya existe una convocatoria con el mismo titulo (ignorando mayusculas) y fuente.
     * Usado para evitar duplicados al importar desde BDNS.
     * Spring Data JPA genera la implementacion automaticamente por convención de nombre.
     */
    boolean existsByTituloIgnoreCaseAndFuente(String titulo, String fuente);

    /**
     * Filtra convocatorias por sector y/o ubicación y/o tipo.
     * Los parámetros nulos o vacíos se ignoran.
     */
    @Query("SELECT c FROM Convocatoria c WHERE " +
           "(:sector IS NULL OR :sector = '' OR c.sector = :sector) AND " +
           "(:ubicacion IS NULL OR :ubicacion = '' OR c.ubicacion = :ubicacion) AND " +
           "(:tipo IS NULL OR :tipo = '' OR c.tipo = :tipo)")
    List<Convocatoria> filtrar(@Param("sector") String sector,
                               @Param("ubicacion") String ubicacion,
                               @Param("tipo") String tipo);

    /**
     * Devuelve los sectores distintos registrados (para el selector de filtros).
     */
    @Query("SELECT DISTINCT c.sector FROM Convocatoria c WHERE c.sector IS NOT NULL ORDER BY c.sector")
    List<String> findSectoresDistintos();

    /**
     * Devuelve los tipos distintos registrados (para el selector de filtros).
     */
    @Query("SELECT DISTINCT c.tipo FROM Convocatoria c WHERE c.tipo IS NOT NULL ORDER BY c.tipo")
    List<String> findTiposDistintos();
}

