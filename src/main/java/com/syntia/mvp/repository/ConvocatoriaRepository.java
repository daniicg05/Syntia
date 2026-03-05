package com.syntia.mvp.repository;

import com.syntia.mvp.model.Convocatoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Convocatoria.
 */
@Repository
public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Long> {

    /**
     * Busca convocatorias por sector.
     * @param sector sector de la convocatoria
     * @return lista de convocatorias del sector
     */
    List<Convocatoria> findBySector(String sector);

    /**
     * Busca convocatorias por ubicación.
     * @param ubicacion ubicación de la convocatoria
     * @return lista de convocatorias de la ubicación
     */
    List<Convocatoria> findByUbicacion(String ubicacion);
}

