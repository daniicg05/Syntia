package com.syntia.mvp.service;

import com.syntia.mvp.model.Convocatoria;
import com.syntia.mvp.model.Perfil;
import com.syntia.mvp.model.Proyecto;
import com.syntia.mvp.model.Recomendacion;
import com.syntia.mvp.model.dto.ConvocatoriaDTO;
import com.syntia.mvp.repository.ConvocatoriaRepository;
import com.syntia.mvp.repository.RecomendacionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor de matching de Syntia.
 * <p>
 * Flujo:
 * <ol>
 *   <li>OpenAI lee el perfil + proyecto y genera keywords de búsqueda.</li>
 *   <li>Se consulta la API BDNS directamente con esas keywords (sin acumular en BD).</li>
 *   <li>OpenAI evalúa cada resultado y le asigna puntuación 0-100 + explicación.</li>
 *   <li>Solo las convocatorias que superan el umbral se persisten en BD y se guardan como recomendación.</li>
 * </ol>
 */
@Slf4j
@Service
public class MotorMatchingService {

    /** Puntuación mínima (0-100) para que una convocatoria se guarde como recomendación. */
    private static final int UMBRAL_RECOMENDACION = 10;

    /** Máximo de resultados a traer de BDNS por cada keyword. */
    private static final int RESULTADOS_POR_KEYWORD = 25;

    /** Máximo de candidatas únicas a evaluar con IA (evita llamadas excesivas a OpenAI). */
    private static final int MAX_CANDIDATAS_IA = 30;

    private final ConvocatoriaRepository convocatoriaRepository;
    private final RecomendacionRepository recomendacionRepository;
    private final PerfilService perfilService;
    private final OpenAiMatchingService openAiMatchingService;
    private final BdnsClientService bdnsClientService;

    public MotorMatchingService(ConvocatoriaRepository convocatoriaRepository,
                                RecomendacionRepository recomendacionRepository,
                                PerfilService perfilService,
                                OpenAiMatchingService openAiMatchingService,
                                BdnsClientService bdnsClientService) {
        this.convocatoriaRepository = convocatoriaRepository;
        this.recomendacionRepository = recomendacionRepository;
        this.perfilService = perfilService;
        this.openAiMatchingService = openAiMatchingService;
        this.bdnsClientService = bdnsClientService;
    }

    /**
     * Genera y persiste recomendaciones para un proyecto.
     *
     * @param proyecto proyecto del usuario
     * @return lista de recomendaciones ordenadas por puntuación desc
     */
    @Transactional
    public List<Recomendacion> generarRecomendaciones(Proyecto proyecto) {
        // Limpiar recomendaciones anteriores
        recomendacionRepository.deleteByProyectoId(proyecto.getId());

        // 1. Cargar perfil del usuario
        Perfil perfil = perfilService.obtenerPerfil(proyecto.getUsuario().getId()).orElse(null);

        // 2. OpenAI genera keywords basadas en perfil + proyecto
        List<String> keywords = generarKeywords(proyecto, perfil);
        log.info("Keywords generadas para proyecto {}: {}", proyecto.getId(), keywords);

        // 3. Buscar en API BDNS directamente con esas keywords (deduplicando por título)
        Map<String, ConvocatoriaDTO> candidatasUnicas = buscarEnBdns(keywords);
        log.info("Candidatas únicas obtenidas de BDNS: {}", candidatasUnicas.size());

        if (candidatasUnicas.isEmpty()) {
            log.warn("⚠ BDNS devolvió 0 candidatas para todas las keywords: {}. " +
                     "¿Está accesible la API de infosubvenciones.es?", keywords);
        }

        // 4. Limitar a MAX_CANDIDATAS_IA para no hacer demasiadas llamadas a OpenAI
        List<ConvocatoriaDTO> aEvaluar = candidatasUnicas.values().stream()
                .limit(MAX_CANDIDATAS_IA)
                .toList();

        // 5. Evaluar cada candidata con OpenAI y persistir solo las que superen el umbral
        List<Recomendacion> recomendaciones = new ArrayList<>();
        int fallosOpenAi = 0;
        int descartadasPorUmbral = 0;
        for (ConvocatoriaDTO dto : aEvaluar) {
            try {
                // Obtener detalle completo de la convocatoria desde la API BDNS
                // (objeto, beneficiarios, requisitos, bases reguladoras, etc.)
                String detalleTexto = null;
                if (dto.getIdBdns() != null) {
                    detalleTexto = bdnsClientService.obtenerDetalleTexto(dto.getIdBdns());
                    if (detalleTexto != null) {
                        log.debug("Detalle BDNS obtenido para '{}': {} chars", dto.getTitulo(), detalleTexto.length());
                    } else {
                        log.debug("Detalle BDNS no disponible para '{}', usando solo título", dto.getTitulo());
                    }
                }

                // Construir entidad temporal (sin ID) para pasarla a OpenAI
                Convocatoria temporal = dtoAEntidad(dto);
                OpenAiMatchingService.ResultadoIA resultado =
                        openAiMatchingService.analizar(proyecto, perfil, temporal, detalleTexto);

                if (resultado.puntuacion() >= UMBRAL_RECOMENDACION) {
                    // Enriquecer el DTO con el sector inferido por OpenAI antes de persistir
                    if (resultado.sector() != null && (dto.getSector() == null || dto.getSector().isBlank())) {
                        dto.setSector(resultado.sector());
                    }
                    // Solo ahora persistimos la convocatoria en BD
                    Convocatoria persistida = persistirConvocatoria(dto);
                    Recomendacion rec = Recomendacion.builder()
                            .proyecto(proyecto)
                            .convocatoria(persistida)
                            .puntuacion(resultado.puntuacion())
                            .explicacion(resultado.explicacion())
                            .guia(resultado.guia())
                            .usadaIa(true)
                            .build();
                    recomendaciones.add(recomendacionRepository.save(rec));
                    log.info("Recomendación guardada: puntuacion={} sector='{}' titulo='{}'",
                            resultado.puntuacion(), dto.getSector(), dto.getTitulo());
                } else {
                    descartadasPorUmbral++;
                    log.debug("Descartada por umbral ({}< {}): '{}'",
                            resultado.puntuacion(), UMBRAL_RECOMENDACION, dto.getTitulo());
                }
            } catch (OpenAiClient.OpenAiUnavailableException e) {
                fallosOpenAi++;
                log.warn("OpenAI no disponible para '{}': {}", dto.getTitulo(), e.getMessage());
            } catch (Exception e) {
                log.warn("Error evaluando convocatoria '{}': {}", dto.getTitulo(), e.getMessage());
            }
        }

        if (fallosOpenAi > 0) {
            log.error("⚠ OpenAI falló en {}/{} evaluaciones. ¿Está configurada la API key (OPENAI_API_KEY)?",
                    fallosOpenAi, aEvaluar.size());
        }
        if (descartadasPorUmbral > 0) {
            log.info("Descartadas por umbral (<{}): {}", UMBRAL_RECOMENDACION, descartadasPorUmbral);
        }

        // Si TODAS las evaluaciones fallaron por OpenAI y no hay recomendaciones,
        // lanzar excepción para que el controlador muestre un mensaje adecuado
        if (recomendaciones.isEmpty() && fallosOpenAi > 0 && fallosOpenAi == aEvaluar.size()) {
            throw new OpenAiClient.OpenAiUnavailableException(
                    "OpenAI no disponible: falló en las " + fallosOpenAi + " evaluaciones. " +
                    "Verifica que la variable OPENAI_API_KEY esté configurada correctamente.");
        }

        recomendaciones.sort((a, b) -> Integer.compare(b.getPuntuacion(), a.getPuntuacion()));
        log.info("Matching completado: proyecto={} keywords={} candidatas={} recomendaciones={}",
                proyecto.getId(), keywords.size(), aEvaluar.size(), recomendaciones.size());

        return recomendaciones;
    }

    // ── Keywords ─────────────────────────────────────────────────────────────

    private List<String> generarKeywords(Proyecto proyecto, Perfil perfil) {
        try {
            return openAiMatchingService.generarKeywordsBusqueda(proyecto, perfil);
        } catch (Exception e) {
            log.warn("Error generando keywords con OpenAI, usando datos básicos del proyecto: {}", e.getMessage());
            return generarKeywordsBasicas(proyecto, perfil);
        }
    }

    private List<String> generarKeywordsBasicas(Proyecto proyecto, Perfil perfil) {
        List<String> kw = new ArrayList<>();
        if (proyecto != null) {
            if (proyecto.getSector() != null && !proyecto.getSector().isBlank())
                kw.add(proyecto.getSector());
            if (proyecto.getNombre() != null && !proyecto.getNombre().isBlank())
                kw.add(proyecto.getNombre());
            if (proyecto.getUbicacion() != null && !proyecto.getUbicacion().isBlank())
                kw.add("subvencion " + proyecto.getUbicacion());
        }
        if (perfil != null) {
            if (perfil.getTipoEntidad() != null && !perfil.getTipoEntidad().isBlank())
                kw.add(perfil.getTipoEntidad() + " subvencion");
            if (perfil.getSector() != null && !perfil.getSector().isBlank())
                kw.add("ayuda " + perfil.getSector());
        }
        // Fallbacks genéricos siempre útiles
        kw.add("subvencion pyme");
        kw.add("ayuda empresa innovacion");
        return kw;
    }

    // ── Búsqueda en BDNS ─────────────────────────────────────────────────────

    /**
     * Busca en la API BDNS con cada keyword y devuelve un mapa título→DTO deduplicado.
     */
    private Map<String, ConvocatoriaDTO> buscarEnBdns(List<String> keywords) {
        Map<String, ConvocatoriaDTO> resultado = new LinkedHashMap<>();
        LocalDate hoy = LocalDate.now();
        for (String kw : keywords) {
            try {
                List<ConvocatoriaDTO> encontradas = bdnsClientService.buscarPorTexto(kw, 0, RESULTADOS_POR_KEYWORD);
                for (ConvocatoriaDTO dto : encontradas) {
                    if (dto.getTitulo() == null) continue;
                    if (resultado.containsKey(dto.getTitulo())) continue;
                    // Descartar SOLO las que tienen fecha de cierre conocida y ya pasó
                    // (null = sin plazo definido = tratar como abierta)
                    if (dto.getFechaCierre() != null && dto.getFechaCierre().isBefore(hoy)) {
                        log.debug("Descartada por caducada: '{}' cierre={}", dto.getTitulo(), dto.getFechaCierre());
                        continue;
                    }
                    resultado.put(dto.getTitulo(), dto);
                }
                log.info("BDNS '{}': {} resultados ({} vigentes acumuladas)", kw, encontradas.size(), resultado.size());
            } catch (Exception e) {
                log.warn("Error consultando BDNS con keyword '{}': {}", kw, e.getMessage());
            }
        }
        return resultado;
    }

    // ── Persistencia selectiva ────────────────────────────────────────────────

    /**
     * Persiste una convocatoria en BD solo si no existe ya (por título + fuente).
     * Devuelve la entidad con ID válido para usarla en la recomendación.
     */
    private Convocatoria persistirConvocatoria(ConvocatoriaDTO dto) {
        return convocatoriaRepository
                .findByTituloIgnoreCaseAndFuente(dto.getTitulo(), dto.getFuente())
                .map(existente -> {
                    boolean changed = false;
                    if (existente.getSector() == null && dto.getSector() != null) {
                        existente.setSector(dto.getSector());
                        changed = true;
                    }
                    if (existente.getIdBdns() == null && dto.getIdBdns() != null) {
                        existente.setIdBdns(dto.getIdBdns());
                        changed = true;
                    }
                    if (existente.getNumeroConvocatoria() == null && dto.getNumeroConvocatoria() != null) {
                        existente.setNumeroConvocatoria(dto.getNumeroConvocatoria());
                        existente.setUrlOficial(dto.getUrlOficial()); // URL con numConv siempre fiable
                        changed = true;
                    }
                    return changed ? convocatoriaRepository.save(existente) : existente;
                })
                .orElseGet(() -> convocatoriaRepository.save(dtoAEntidad(dto)));
    }

    private Convocatoria dtoAEntidad(ConvocatoriaDTO dto) {
        return Convocatoria.builder()
                .titulo(dto.getTitulo())
                .tipo(dto.getTipo())
                .sector(dto.getSector())
                .ubicacion(dto.getUbicacion())
                .urlOficial(dto.getUrlOficial())
                .fuente(dto.getFuente())
                .idBdns(dto.getIdBdns())
                .numeroConvocatoria(dto.getNumeroConvocatoria())
                .fechaCierre(dto.getFechaCierre())
                .build();
    }
}
