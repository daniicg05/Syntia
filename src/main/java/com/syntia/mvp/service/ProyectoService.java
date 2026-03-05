package com.syntia.mvp.service;

import com.syntia.mvp.model.Proyecto;
import com.syntia.mvp.model.Usuario;
import com.syntia.mvp.model.dto.ProyectoDTO;
import com.syntia.mvp.repository.ProyectoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de lógica de negocio para la gestión de proyectos.
 * <p>
 * Decisión arquitectónica: la verificación de propiedad (el proyecto pertenece
 * al usuario autenticado) se realiza en el servicio y no en el controller,
 * siguiendo el principio de que las reglas de negocio viven en la capa de servicio.
 */
@Service
public class ProyectoService {

    private final ProyectoRepository proyectoRepository;

    public ProyectoService(ProyectoRepository proyectoRepository) {
        this.proyectoRepository = proyectoRepository;
    }

    /**
     * Obtiene todos los proyectos del usuario autenticado.
     *
     * @param usuarioId ID del usuario
     * @return lista de proyectos del usuario
     */
    public List<Proyecto> obtenerProyectos(Long usuarioId) {
        return proyectoRepository.findByUsuarioId(usuarioId);
    }

    /**
     * Obtiene un proyecto por ID verificando que pertenece al usuario.
     *
     * @param id        ID del proyecto
     * @param usuarioId ID del usuario autenticado
     * @return el proyecto si existe y pertenece al usuario
     * @throws EntityNotFoundException si el proyecto no existe
     * @throws AccessDeniedException   si el proyecto pertenece a otro usuario
     */
    public Proyecto obtenerPorId(Long id, Long usuarioId) {
        Proyecto proyecto = proyectoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proyecto no encontrado: " + id));
        verificarPropiedad(proyecto, usuarioId);
        return proyecto;
    }

    /**
     * Crea un nuevo proyecto para el usuario autenticado.
     *
     * @param usuario usuario propietario del proyecto
     * @param dto     datos del formulario
     * @return proyecto creado y persistido
     */
    @Transactional
    public Proyecto crear(Usuario usuario, ProyectoDTO dto) {
        Proyecto proyecto = Proyecto.builder()
                .usuario(usuario)
                .nombre(dto.getNombre())
                .sector(dto.getSector())
                .ubicacion(dto.getUbicacion())
                .descripcion(dto.getDescripcion())
                .build();
        return proyectoRepository.save(proyecto);
    }

    /**
     * Actualiza un proyecto existente verificando que pertenece al usuario.
     *
     * @param id        ID del proyecto a actualizar
     * @param usuarioId ID del usuario autenticado
     * @param dto       datos del formulario
     * @return proyecto actualizado
     */
    @Transactional
    public Proyecto actualizar(Long id, Long usuarioId, ProyectoDTO dto) {
        Proyecto proyecto = obtenerPorId(id, usuarioId);
        proyecto.setNombre(dto.getNombre());
        proyecto.setSector(dto.getSector());
        proyecto.setUbicacion(dto.getUbicacion());
        proyecto.setDescripcion(dto.getDescripcion());
        return proyectoRepository.save(proyecto);
    }

    /**
     * Elimina un proyecto verificando que pertenece al usuario.
     *
     * @param id        ID del proyecto a eliminar
     * @param usuarioId ID del usuario autenticado
     */
    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        Proyecto proyecto = obtenerPorId(id, usuarioId);
        proyectoRepository.delete(proyecto);
    }

    /**
     * Convierte un {@link Proyecto} en su {@link ProyectoDTO} equivalente.
     * Útil para precargar el formulario de edición.
     *
     * @param proyecto entidad
     * @return DTO con los datos del proyecto
     */
    public ProyectoDTO toDTO(Proyecto proyecto) {
        ProyectoDTO dto = new ProyectoDTO();
        dto.setNombre(proyecto.getNombre());
        dto.setSector(proyecto.getSector());
        dto.setUbicacion(proyecto.getUbicacion());
        dto.setDescripcion(proyecto.getDescripcion());
        return dto;
    }

    /**
     * Verifica que el proyecto pertenece al usuario autenticado.
     * Lanza {@link AccessDeniedException} si no es así.
     */
    private void verificarPropiedad(Proyecto proyecto, Long usuarioId) {
        if (!proyecto.getUsuario().getId().equals(usuarioId)) {
            throw new AccessDeniedException("No tienes permiso para acceder a este proyecto.");
        }
    }
}

