package com.syntia.mvp.service;

import com.syntia.mvp.model.Convocatoria;
import com.syntia.mvp.model.Perfil;
import com.syntia.mvp.model.Proyecto;
import com.syntia.mvp.model.Recomendacion;
import com.syntia.mvp.model.dto.ConvocatoriaDTO;
import com.syntia.mvp.repository.ConvocatoriaRepository;
import com.syntia.mvp.repository.RecomendacionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private static final int UMBRAL_RECOMENDACION = 20;

    /** Máximo de resultados a traer de BDNS por cada keyword. */
    private static final int RESULTADOS_POR_KEYWORD = 15;

    /** Máximo de candidatas únicas a evaluar con IA (evita llamadas excesivas a OpenAI). */
    private static final int MAX_CANDIDATAS_IA = 15;

    private final ConvocatoriaRepository convocatoriaRepository;
    private final RecomendacionRepository recomendacionRepository;
    private final PerfilService perfilService;
    private final OpenAiMatchingService openAiMatchingService;
    private final BdnsClientService bdnsClientService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public MotorMatchingService(ConvocatoriaRepository convocatoriaRepository,
                                RecomendacionRepository recomendacionRepository,
                                PerfilService perfilService,
                                OpenAiMatchingService openAiMatchingService,
                                BdnsClientService bdnsClientService,
                                PlatformTransactionManager transactionManager) {
        this.convocatoriaRepository = convocatoriaRepository;
        this.recomendacionRepository = recomendacionRepository;
        this.perfilService = perfilService;
        this.openAiMatchingService = openAiMatchingService;
        this.bdnsClientService = bdnsClientService;
        this.objectMapper = new ObjectMapper();
        this.transactionTemplate = new TransactionTemplate(transactionManager);
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

    // ── Streaming con SSE ────────────────────────────────────────────────────

    /**
     * Genera recomendaciones emitiendo eventos SSE en tiempo real.
     * <p>
     * NOTA sobre @Transactional: este método se llama desde un CompletableFuture.runAsync(),
     * por lo que la anotación @Transactional NO funcionará a través del proxy de Spring.
     * Se usa gestión transaccional manual (autocommit por operación JPA) en su lugar.
     * Cada save/delete de los repositorios se ejecuta como transacción individual,
     * lo cual es aceptable para este flujo de larga duración con SSE.
     *
     * @param proyecto proyecto del usuario
     * @param emitter  SseEmitter para enviar eventos al navegador
     */
    public void generarRecomendacionesStream(Proyecto proyecto, SseEmitter emitter) {
        try {
            // 1. Limpiar recomendaciones anteriores (requiere transacción)
            transactionTemplate.executeWithoutResult(status ->
                    recomendacionRepository.deleteByProyectoId(proyecto.getId()));
            enviarEvento(emitter, "estado", "Limpiando recomendaciones anteriores...");

            // 2. Cargar perfil
            Perfil perfil = perfilService.obtenerPerfil(proyecto.getUsuario().getId()).orElse(null);

            // 3. Generar keywords con OpenAI
            enviarEvento(emitter, "estado", "🔍 Analizando tu proyecto con IA...");
            List<String> keywords = generarKeywords(proyecto, perfil);
            log.info("SSE: Keywords generadas para proyecto {}: {}", proyecto.getId(), keywords);
            enviarEvento(emitter, "keywords",
                    Map.of("total", keywords.size(), "keywords", keywords));

            // 4. Buscar en BDNS
            enviarEvento(emitter, "estado", "🌐 Buscando convocatorias en la BDNS...");
            Map<String, ConvocatoriaDTO> candidatasUnicas = buscarEnBdns(keywords);
            log.info("SSE: Candidatas únicas obtenidas de BDNS: {}", candidatasUnicas.size());
            enviarEvento(emitter, "busqueda",
                    Map.of("candidatas", candidatasUnicas.size()));

            if (candidatasUnicas.isEmpty()) {
                enviarEvento(emitter, "estado",
                        "⚠️ No se encontraron convocatorias vigentes para tus keywords.");
                enviarEvento(emitter, "completado",
                        Map.of("totalRecomendaciones", 0, "totalEvaluadas", 0,
                               "descartadas", 0, "errores", 0));
                return;
            }

            // 5. Limitar candidatas
            List<ConvocatoriaDTO> aEvaluar = candidatasUnicas.values().stream()
                    .limit(MAX_CANDIDATAS_IA)
                    .toList();
            enviarEvento(emitter, "estado",
                    "🤖 Evaluando " + aEvaluar.size() + " convocatorias con IA...");

            // 6. Evaluar cada candidata, emitiendo resultados parciales
            List<Recomendacion> recomendaciones = new ArrayList<>();
            int procesadas = 0;
            int fallosOpenAi = 0;
            int descartadasPorUmbral = 0;

            for (ConvocatoriaDTO dto : aEvaluar) {
                procesadas++;
                enviarEvento(emitter, "progreso", Map.of(
                        "actual", procesadas,
                        "total", aEvaluar.size(),
                        "titulo", dto.getTitulo() != null ? dto.getTitulo() : "Sin título"
                ));

                try {
                    // Obtener detalle de la convocatoria
                    String detalleTexto = null;
                    if (dto.getIdBdns() != null) {
                        detalleTexto = bdnsClientService.obtenerDetalleTexto(dto.getIdBdns());
                    }

                    // Evaluar con OpenAI
                    Convocatoria temporal = dtoAEntidad(dto);
                    OpenAiMatchingService.ResultadoIA resultado =
                            openAiMatchingService.analizar(proyecto, perfil, temporal, detalleTexto);

                    if (resultado.puntuacion() >= UMBRAL_RECOMENDACION) {
                        if (resultado.sector() != null && (dto.getSector() == null || dto.getSector().isBlank())) {
                            dto.setSector(resultado.sector());
                        }
                        // Persistir en transacción programática (estamos fuera del proxy @Transactional)
                        final String explicacion = resultado.explicacion();
                        final String guia = resultado.guia();
                        final int puntuacion = resultado.puntuacion();
                        Recomendacion rec = transactionTemplate.execute(status -> {
                            Convocatoria persistida = persistirConvocatoria(dto);
                            Recomendacion r = Recomendacion.builder()
                                    .proyecto(proyecto)
                                    .convocatoria(persistida)
                                    .puntuacion(puntuacion)
                                    .explicacion(explicacion)
                                    .guia(guia)
                                    .usadaIa(true)
                                    .build();
                            return recomendacionRepository.save(r);
                        });
                        recomendaciones.add(rec);

                        // Enviar resultado parcial al navegador inmediatamente
                        enviarEvento(emitter, "resultado", Map.of(
                                "titulo", dto.getTitulo() != null ? dto.getTitulo() : "Sin título",
                                "puntuacion", resultado.puntuacion(),
                                "explicacion", resultado.explicacion() != null ? resultado.explicacion() : "",
                                "tipo", dto.getTipo() != null ? dto.getTipo() : "",
                                "sector", dto.getSector() != null ? dto.getSector() : "",
                                "ubicacion", dto.getUbicacion() != null ? dto.getUbicacion() : "",
                                "urlOficial", dto.getUrlOficial() != null ? dto.getUrlOficial() : "",
                                "fuente", dto.getFuente() != null ? dto.getFuente() : "",
                                "totalEncontradas", recomendaciones.size()
                        ));
                        log.info("SSE resultado: puntuacion={} titulo='{}'",
                                resultado.puntuacion(), dto.getTitulo());
                    } else {
                        descartadasPorUmbral++;
                    }
                } catch (OpenAiClient.OpenAiUnavailableException e) {
                    fallosOpenAi++;
                    log.warn("SSE: OpenAI no disponible para '{}': {}", dto.getTitulo(), e.getMessage());
                } catch (Exception e) {
                    log.warn("SSE: Error evaluando '{}': {}", dto.getTitulo(), e.getMessage());
                }
            }

            // 7. Resumen final
            enviarEvento(emitter, "completado", Map.of(
                    "totalRecomendaciones", recomendaciones.size(),
                    "totalEvaluadas", aEvaluar.size(),
                    "descartadas", descartadasPorUmbral,
                    "errores", fallosOpenAi
            ));

            log.info("SSE matching completado: proyecto={} recomendaciones={}",
                    proyecto.getId(), recomendaciones.size());

        } catch (Exception e) {
            log.error("SSE: Error en generarRecomendacionesStream: {}", e.getMessage(), e);
            enviarEvento(emitter, "error", "Error interno: " + e.getMessage());
        }
    }

    /**
     * Envía un evento SSE al cliente. Silencia errores si la conexión ya se cerró.
     */
    private void enviarEvento(SseEmitter emitter, String tipo, Object datos) {
        try {
            String json;
            if (datos instanceof String s) {
                json = "\"" + s.replace("\"", "\\\"") + "\"";
            } else {
                json = objectMapper.writeValueAsString(datos);
            }
            emitter.send(SseEmitter.event()
                    .name(tipo)
                    .data(json, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.debug("Error enviando evento SSE '{}': {}", tipo, e.getMessage());
        }
    }
}
