package com.syntia.mvp.service;

import com.syntia.mvp.model.Convocatoria;
import com.syntia.mvp.model.dto.ConvocatoriaDTO;
import com.syntia.mvp.repository.ConvocatoriaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de lógica de negocio para la gestión de convocatorias.
 * Usado principalmente por el panel administrativo y el motor de matching.
 */
@Slf4j
@Service
public class ConvocatoriaService {

    private final ConvocatoriaRepository convocatoriaRepository;
    private final BdnsClientService bdnsClientService;

    public ConvocatoriaService(ConvocatoriaRepository convocatoriaRepository,
                               BdnsClientService bdnsClientService) {
        this.convocatoriaRepository = convocatoriaRepository;
        this.bdnsClientService = bdnsClientService;
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

    /**
     * Importa convocatorias desde la API publica de BDNS y las persiste evitando duplicados.
     * Dos convocatorias se consideran duplicadas si tienen el mismo titulo y fuente.
     *
     * @param pagina numero de pagina BDNS (0-indexed)
     * @param tamano registros por pagina (max 50)
     * @return numero de convocatorias nuevas importadas (las duplicadas se omiten)
     * @throws BdnsClientService.BdnsException si la API de BDNS no esta disponible
     */
    @Transactional
    public int importarDesdeBdns(int pagina, int tamano) {
        List<ConvocatoriaDTO> importadas = bdnsClientService.importar(pagina, tamano);
        return persistirNuevas(importadas);
    }


    private int persistirNuevas(List<ConvocatoriaDTO> importadas) {
        int nuevas = 0;
        for (ConvocatoriaDTO dto : importadas) {
            boolean existe = convocatoriaRepository.existsByTituloIgnoreCaseAndFuente(dto.getTitulo(), dto.getFuente());
            if (!existe) {
                crear(dto);
                nuevas++;
            }
        }

        log.info("BDNS import: {} importadas, {} nuevas, {} duplicadas omitidas",
                importadas.size(), nuevas, importadas.size() - nuevas);
        return nuevas;
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

