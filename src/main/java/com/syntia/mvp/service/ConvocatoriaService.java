package com.syntia.mvp.service;

import com.syntia.mvp.model.Convocatoria;
import com.syntia.mvp.model.dto.ConvocatoriaDTO;
import com.syntia.mvp.repository.ConvocatoriaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de lógica de negocio para la gestión de convocatorias.
 * Usado principalmente por el panel administrativo y el motor de matching.
 */
@Service
public class ConvocatoriaService {

    private final ConvocatoriaRepository convocatoriaRepository;

    public ConvocatoriaService(ConvocatoriaRepository convocatoriaRepository) {
        this.convocatoriaRepository = convocatoriaRepository;
    }

    /** Obtiene todas las convocatorias registradas. */
    public List<Convocatoria> obtenerTodas() {
        return convocatoriaRepository.findAll();
    }

    /**
     * Obtiene una convocatoria por ID.
     * @throws EntityNotFoundException si no existe
     */
    public Convocatoria obtenerPorId(Long id) {
        return convocatoriaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Convocatoria no encontrada: " + id));
    }

    /** Crea una nueva convocatoria. */
    @Transactional
    public Convocatoria crear(ConvocatoriaDTO dto) {
        Convocatoria c = new Convocatoria();
        c.setTitulo(dto.getTitulo());
        c.setTipo(dto.getTipo());
        c.setSector(dto.getSector());
        c.setUbicacion(dto.getUbicacion());
        c.setUrlOficial(dto.getUrlOficial());
        c.setFuente(dto.getFuente());
        c.setFechaCierre(dto.getFechaCierre());
        return convocatoriaRepository.save(c);
    }

    /**
     * Actualiza una convocatoria existente.
     * @throws EntityNotFoundException si no existe
     */
    @Transactional
    public Convocatoria actualizar(Long id, ConvocatoriaDTO dto) {
        Convocatoria c = obtenerPorId(id);
        c.setTitulo(dto.getTitulo());
        c.setTipo(dto.getTipo());
        c.setSector(dto.getSector());
        c.setUbicacion(dto.getUbicacion());
        c.setUrlOficial(dto.getUrlOficial());
        c.setFuente(dto.getFuente());
        c.setFechaCierre(dto.getFechaCierre());
        return convocatoriaRepository.save(c);
    }

    /**
     * Elimina una convocatoria por ID.
     * @throws EntityNotFoundException si no existe
     */
    @Transactional
    public void eliminar(Long id) {
        Convocatoria c = obtenerPorId(id);
        convocatoriaRepository.delete(c);
    }

    /** Convierte una entidad a DTO para precargar formularios de edición. */
    public ConvocatoriaDTO toDTO(Convocatoria c) {
        ConvocatoriaDTO dto = new ConvocatoriaDTO();
        dto.setTitulo(c.getTitulo());
        dto.setTipo(c.getTipo());
        dto.setSector(c.getSector());
        dto.setUbicacion(c.getUbicacion());
        dto.setUrlOficial(c.getUrlOficial());
        dto.setFuente(c.getFuente());
        dto.setFechaCierre(c.getFechaCierre());
        return dto;
    }
}

