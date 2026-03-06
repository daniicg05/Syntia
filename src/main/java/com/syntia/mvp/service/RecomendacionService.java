package com.syntia.mvp.service;

import com.syntia.mvp.model.Recomendacion;
import com.syntia.mvp.model.dto.RecomendacionDTO;
import com.syntia.mvp.repository.RecomendacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para consulta y transformación de recomendaciones.
 * La generación de recomendaciones se delega a {@link MotorMatchingService}.
 * Este servicio se ocupa de la lectura y conversión a DTO para las vistas.
 */
@Service
public class RecomendacionService {

    private final RecomendacionRepository recomendacionRepository;

    public RecomendacionService(RecomendacionRepository recomendacionRepository) {
        this.recomendacionRepository = recomendacionRepository;
    }

    /**
     * Obtiene las recomendaciones de un proyecto ordenadas por puntuación descendente.
     *
     * @param proyectoId ID del proyecto
     * @return lista de RecomendacionDTO lista para la vista
     */
    public List<RecomendacionDTO> obtenerPorProyecto(Long proyectoId) {
        return recomendacionRepository
                .findByProyectoIdOrderByPuntuacionDesc(proyectoId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Cuenta el número de recomendaciones de un proyecto.
     */
    public long contarPorProyecto(Long proyectoId) {
        return recomendacionRepository.countByProyectoId(proyectoId);
    }

    /**
     * Filtra recomendaciones de un proyecto por tipo, sector y ubicacion.
     * El filtrado se delega a la BD (no en memoria).
     * Los parametros nulos o vacios se ignoran.
     *
     * @param proyectoId ID del proyecto (obligatorio)
     * @param tipo       filtro por tipo de convocatoria (opcional)
     * @param sector     filtro por sector (opcional)
     * @param ubicacion  filtro por ubicacion (opcional)
     * @return lista de RecomendacionDTO ordenada por puntuacion desc
     */
    public List<RecomendacionDTO> filtrar(Long proyectoId, String tipo, String sector, String ubicacion) {
        return recomendacionRepository
                .filtrar(proyectoId,
                         (tipo      != null && !tipo.isBlank())      ? tipo      : null,
                         (sector    != null && !sector.isBlank())    ? sector    : null,
                         (ubicacion != null && !ubicacion.isBlank()) ? ubicacion : null)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los tipos distintos de convocatoria para las recomendaciones de un proyecto.
     * Usado para poblar los selectores de filtro en la vista.
     */
    public List<String> obtenerTiposDistintos(Long proyectoId) {
        return recomendacionRepository.findTiposDistintosByProyectoId(proyectoId);
    }

    /**
     * Obtiene los sectores distintos para las recomendaciones de un proyecto.
     * Usado para poblar los selectores de filtro en la vista.
     */
    public List<String> obtenerSectoresDistintos(Long proyectoId) {
        return recomendacionRepository.findSectoresDistintosByProyectoId(proyectoId);
    }

    /**
     * Convierte una entidad {@link Recomendacion} a {@link RecomendacionDTO}.
     * Aplana los datos de la convocatoria asociada para evitar LazyInitializationException.
     */
    public RecomendacionDTO toDTO(Recomendacion rec) {
        RecomendacionDTO dto = new RecomendacionDTO();
        dto.setId(rec.getId());
        dto.setPuntuacion(rec.getPuntuacion());
        dto.setExplicacion(rec.getExplicacion());
        dto.setUsadaIa(rec.isUsadaIa());
        dto.setConvocatoriaId(rec.getConvocatoria().getId());
        dto.setTitulo(rec.getConvocatoria().getTitulo());
        dto.setTipo(rec.getConvocatoria().getTipo());
        dto.setSector(rec.getConvocatoria().getSector());
        dto.setUbicacion(rec.getConvocatoria().getUbicacion());
        dto.setUrlOficial(rec.getConvocatoria().getUrlOficial());
        dto.setFuente(rec.getConvocatoria().getFuente());
        dto.setFechaCierre(rec.getConvocatoria().getFechaCierre());
        return dto;
    }
}

