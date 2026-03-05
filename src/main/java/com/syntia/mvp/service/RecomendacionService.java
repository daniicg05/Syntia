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
     * Convierte una entidad {@link Recomendacion} a {@link RecomendacionDTO}.
     * Aplana los datos de la convocatoria asociada para evitar LazyInitializationException.
     */
    public RecomendacionDTO toDTO(Recomendacion rec) {
        RecomendacionDTO dto = new RecomendacionDTO();
        dto.setId(rec.getId());
        dto.setPuntuacion(rec.getPuntuacion());
        dto.setExplicacion(rec.getExplicacion());
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

