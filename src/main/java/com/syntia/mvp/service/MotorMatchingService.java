package com.syntia.mvp.service;

import com.syntia.mvp.model.Convocatoria;
import com.syntia.mvp.model.Perfil;
import com.syntia.mvp.model.Proyecto;
import com.syntia.mvp.model.Recomendacion;
import com.syntia.mvp.model.dto.ConvocatoriaDTO;
import com.syntia.mvp.model.dto.FiltrosBdns;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor de matching de Syntia.
 * <p>
 * Flujo v4.0.0 (BDNS-First):
 * <ol>
 *   <li>{@link BdnsFiltrosBuilder} construye filtros determinísticos desde proyecto+perfil (sin IA).</li>
 *   <li>{@link BdnsClientService#buscarPorFiltros} consulta la API BDNS con parámetros estructurados.</li>
 *   <li>Deduplicación + pre-filtro geográfico como safety net.</li>
 *   <li>OpenAI evalúa cada candidata y asigna puntuación 0-100 + explicación + guía.</li>
 *   <li>Solo las convocatorias ≥ umbral se persisten en BD.</li>
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
     * <p>
     * v4.0.0 — Pipeline BDNS-First: los filtros se construyen determinísticamente
     * a partir de los campos del proyecto y perfil, sin depender de OpenAI.
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

        // 2. BDNS-First: construir filtros determinísticos (sin IA)
        FiltrosBdns filtros = BdnsFiltrosBuilder.construir(proyecto, perfil);
        log.info("BDNS-First filtros para proyecto {}: descripcion='{}' ccaa='{}'",
                proyecto.getId(), filtros.descripcion(), filtros.nivel2());

        // 3. Buscar en API BDNS con filtros estructurados
        List<ConvocatoriaDTO> candidatasBdns = bdnsClientService.buscarPorFiltros(filtros);
        Map<String, ConvocatoriaDTO> candidatasUnicas = deduplicarYFiltrarCaducadas(candidatasBdns);
        log.info("Candidatas únicas obtenidas de BDNS: {}", candidatasUnicas.size());

        if (candidatasUnicas.isEmpty()) {
            log.warn("⚠ BDNS devolvió 0 candidatas con filtros: {}. " +
                     "¿Está accesible la API de infosubvenciones.es?", filtros);
        }

        // 4. Pre-filtro geográfico (safety net) + limitar a MAX_CANDIDATAS_IA
        List<ConvocatoriaDTO> aEvaluar = aplicarPreFiltroGeografico(
                candidatasUnicas, proyecto, perfil);

        // 5. Evaluar cada candidata con OpenAI y persistir solo las que superen el umbral
        // Primero obtener todos los detalles en paralelo para reducir latencia
        Map<String, String> detallesPorId = obtenerDetallesEnParalelo(aEvaluar);

        List<Recomendacion> recomendaciones = new ArrayList<>();
        int fallosOpenAi = 0;
        int descartadasPorUmbral = 0;
        for (ConvocatoriaDTO dto : aEvaluar) {
            try {
                // Obtener detalle completo (ya precargado en paralelo)
                String detalleTexto = dto.getIdBdns() != null
                        ? detallesPorId.get(dto.getIdBdns())
                        : null;
                if (detalleTexto != null) {
                    log.debug("Detalle BDNS disponible para '{}': {} chars", dto.getTitulo(), detalleTexto.length());
                } else {
                    log.debug("Detalle BDNS no disponible para '{}', usando solo título", dto.getTitulo());
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
        log.info("Matching completado: proyecto={} filtros='{}|{}' candidatas={} recomendaciones={}",
                proyecto.getId(), filtros.descripcion(), filtros.nivel2(),
                aEvaluar.size(), recomendaciones.size());

        return recomendaciones;
    }

    // ── Helpers BDNS-First (v4.0.0) ─────────────────────────────────────────

    /**
     * Deduplica una lista de ConvocatoriaDTO por idBdns + título, y descarta caducadas.
     *
     * @param candidatas lista bruta de BDNS
     * @return mapa título→DTO deduplicado, sin caducadas
     */
    private Map<String, ConvocatoriaDTO> deduplicarYFiltrarCaducadas(List<ConvocatoriaDTO> candidatas) {
        Map<String, ConvocatoriaDTO> resultado = new LinkedHashMap<>();
        java.util.Set<String> idsBdnsVistos = new java.util.HashSet<>();
        LocalDate hoy = LocalDate.now();
        for (ConvocatoriaDTO dto : candidatas) {
            if (dto.getTitulo() == null) continue;
            if (dto.getIdBdns() != null && !dto.getIdBdns().isBlank()) {
                if (idsBdnsVistos.contains(dto.getIdBdns())) continue;
                idsBdnsVistos.add(dto.getIdBdns());
            }
            if (resultado.containsKey(dto.getTitulo())) continue;
            if (dto.getFechaCierre() != null && dto.getFechaCierre().isBefore(hoy)) {
                log.debug("Descartada por caducada: '{}' cierre={}", dto.getTitulo(), dto.getFechaCierre());
                continue;
            }
            resultado.put(dto.getTitulo(), dto);
        }
        return resultado;
    }

    /**
     * Aplica pre-filtro geográfico en memoria como segunda capa de seguridad.
     * Descarta convocatorias autonómicas cuya ubicación no coincide con la del usuario.
     * Limita a MAX_CANDIDATAS_IA.
     *
     * @param candidatasUnicas mapa de candidatas deduplicadas
     * @param proyecto         proyecto del usuario
     * @param perfil           perfil del usuario (puede ser null)
     * @return lista filtrada limitada a MAX_CANDIDATAS_IA
     */
    private List<ConvocatoriaDTO> aplicarPreFiltroGeografico(
            Map<String, ConvocatoriaDTO> candidatasUnicas, Proyecto proyecto, Perfil perfil) {
        String ubicacionUsuario = proyecto.getUbicacion();
        if ((ubicacionUsuario == null || ubicacionUsuario.isBlank()) && perfil != null) {
            ubicacionUsuario = perfil.getUbicacion();
        }
        if (ubicacionUsuario != null && !ubicacionUsuario.isBlank()) {
            final String ubiFinal = ubicacionUsuario.toLowerCase().trim();
            List<ConvocatoriaDTO> filtradas = candidatasUnicas.values().stream()
                    .filter(dto -> {
                        String ubiConv = dto.getUbicacion();
                        if (ubiConv == null || ubiConv.isBlank() || "Nacional".equalsIgnoreCase(ubiConv)) {
                            return true;
                        }
                        return ubiConv.toLowerCase().contains(ubiFinal)
                                || ubiFinal.contains(ubiConv.toLowerCase());
                    })
                    .limit(MAX_CANDIDATAS_IA)
                    .toList();
            log.info("Pre-filtro geográfico: {} de {} candidatas pasan (ubicación: {})",
                    filtradas.size(), candidatasUnicas.size(), ubiFinal);
            return filtradas;
        }
        return candidatasUnicas.values().stream()
                .limit(MAX_CANDIDATAS_IA)
                .toList();
    }

    // ── Búsqueda en BDNS (legacy, mantenido como fallback) ────────────────────

    /**
     * Busca en la API BDNS con cada keyword y devuelve un mapa título→DTO deduplicado.
     * Deduplicación doble: primero por idBdns (más fiable), luego por título como fallback.
     * <p>
     * v3.4.0+: Aplica pre-filtro geográfico PRE-búsqueda usando los parámetros nivel1/nivel2
     * de la API BDNS cuando la ubicación del usuario se puede normalizar a una CCAA reconocida.
     * Esto reduce las candidatas irrelevantes ANTES de que lleguen al motor IA.
     *
     * @param keywords  lista de keywords generadas por OpenAI o fallback
     * @param proyecto  proyecto del usuario (para obtener ubicación)
     * @param perfil    perfil de la entidad (fallback de ubicación, puede ser null)
     * @return mapa título→DTO deduplicado con convocatorias vigentes
     */
    private Map<String, ConvocatoriaDTO> buscarEnBdns(List<String> keywords, Proyecto proyecto, Perfil perfil) {
        // v3.4.0: Normalizar ubicación UNA sola vez antes del bucle de keywords
        String ubicacionRaw = (proyecto.getUbicacion() != null && !proyecto.getUbicacion().isBlank())
                ? proyecto.getUbicacion()
                : (perfil != null ? perfil.getUbicacion() : null);
        String ccaaNormalizada = UbicacionNormalizador.normalizarACcaa(ubicacionRaw);
        log.debug("Pre-filtro BDNS activo — CCAA normalizada: {}",
                ccaaNormalizada != null ? ccaaNormalizada : "ninguna (búsqueda nacional)");

        Map<String, ConvocatoriaDTO> resultado = new LinkedHashMap<>();
        java.util.Set<String> idsBdnsVistos = new java.util.HashSet<>();
        LocalDate hoy = LocalDate.now();
        for (String kw : keywords) {
            try {
                List<ConvocatoriaDTO> encontradas = bdnsClientService.buscarPorTextoFiltrado(kw, ccaaNormalizada);
                for (ConvocatoriaDTO dto : encontradas) {
                    if (dto.getTitulo() == null) continue;
                    // Deduplicar por idBdns primero (más fiable que título)
                    if (dto.getIdBdns() != null && !dto.getIdBdns().isBlank()) {
                        if (idsBdnsVistos.contains(dto.getIdBdns())) continue;
                        idsBdnsVistos.add(dto.getIdBdns());
                    }
                    // Deduplicar por título como capa adicional
                    if (resultado.containsKey(dto.getTitulo())) continue;
                    // Descartar SOLO las que tienen fecha de cierre conocida y ya pasó
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

    /**
     * Obtiene en paralelo el detalle BDNS de todas las candidatas.
     * Reduce la latencia de O(n×t) a O(t) donde t es el tiempo de una sola llamada.
     *
     * @param candidatas lista de DTOs a enriquecer con detalle
     * @return mapa idBdns → texto de detalle (null si no disponible)
     */
    private Map<String, String> obtenerDetallesEnParalelo(List<ConvocatoriaDTO> candidatas) {
        Map<String, String> detalles = new ConcurrentHashMap<>();
        if (candidatas.isEmpty()) return detalles;
        int nHilos = Math.min(candidatas.size(), 10);
        // En Java 17 ExecutorService no implementa AutoCloseable; el finally garantiza el shutdown
        @SuppressWarnings("resource")
        ExecutorService executor = Executors.newFixedThreadPool(nHilos);
        try {
            List<CompletableFuture<Void>> futuros = candidatas.stream()
                    .filter(dto -> dto.getIdBdns() != null && !dto.getIdBdns().isBlank())
                    .map(dto -> CompletableFuture.runAsync(() -> {
                        String texto = bdnsClientService.obtenerDetalleTexto(dto.getIdBdns());
                        if (texto != null && !texto.isBlank()) {
                            detalles.put(dto.getIdBdns(), texto);
                        }
                    }, executor))
                    .toList();
            CompletableFuture.allOf(futuros.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.warn("Error en obtención paralela de detalles BDNS: {}", e.getMessage());
        } finally {
            executor.shutdown(); // siempre liberar el pool
        }
        log.info("Detalles BDNS obtenidos en paralelo: {}/{} con contenido", detalles.size(), candidatas.size());
        return detalles;
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

            // 3. BDNS-First: construir filtros determinísticos (sin IA)
            enviarEvento(emitter, "estado", "🔍 Analizando tu proyecto...");
            FiltrosBdns filtros = BdnsFiltrosBuilder.construir(proyecto, perfil);
            log.info("SSE BDNS-First: proyecto={} descripcion='{}' ccaa='{}'",
                    proyecto.getId(), filtros.descripcion(), filtros.nivel2());
            enviarEvento(emitter, "filtros",
                    Map.of("descripcion", filtros.descripcion() != null ? filtros.descripcion() : "",
                           "ccaa", filtros.nivel2() != null ? filtros.nivel2() : "Nacional"));

            // 4. Buscar en BDNS con filtros estructurados
            enviarEvento(emitter, "estado", "🌐 Buscando convocatorias en la BDNS...");
            List<ConvocatoriaDTO> candidatasBdns = bdnsClientService.buscarPorFiltros(filtros);
            Map<String, ConvocatoriaDTO> candidatasUnicas = deduplicarYFiltrarCaducadas(candidatasBdns);
            log.info("SSE: Candidatas únicas obtenidas de BDNS: {}", candidatasUnicas.size());
            enviarEvento(emitter, "busqueda",
                    Map.of("candidatas", candidatasUnicas.size()));

            if (candidatasUnicas.isEmpty()) {
                enviarEvento(emitter, "estado",
                        "⚠️ No se encontraron convocatorias vigentes para tu perfil.");
                enviarEvento(emitter, "completado",
                        Map.of("totalRecomendaciones", 0, "totalEvaluadas", 0,
                               "descartadas", 0, "errores", 0));
                return;
            }

            // 5. Pre-filtro geográfico (safety net)
            List<ConvocatoriaDTO> aEvaluar = aplicarPreFiltroGeografico(
                    candidatasUnicas, proyecto, perfil);
            enviarEvento(emitter, "estado",
                    "🤖 Evaluando " + aEvaluar.size() + " convocatorias con IA...");

            // 6. Obtener detalles BDNS en PARALELO (antes del bucle de evaluación IA)
            enviarEvento(emitter, "estado", "📄 Descargando detalles de convocatorias en paralelo...");
            Map<String, String> detallesPorId = obtenerDetallesEnParalelo(aEvaluar);

            // 7. Evaluar cada candidata, emitiendo resultados parciales
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
                    // Obtener detalle (ya cargado en paralelo, solo lookup en map)
                    String detalleTexto = dto.getIdBdns() != null
                            ? detallesPorId.get(dto.getIdBdns())
                            : null;

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
                        Map<String, Object> resultadoEvento = new LinkedHashMap<>();
                        resultadoEvento.put("titulo", dto.getTitulo() != null ? dto.getTitulo() : "Sin título");
                        resultadoEvento.put("puntuacion", resultado.puntuacion());
                        resultadoEvento.put("explicacion", resultado.explicacion() != null ? resultado.explicacion() : "");
                        resultadoEvento.put("tipo", dto.getTipo() != null ? dto.getTipo() : "");
                        resultadoEvento.put("sector", dto.getSector() != null ? dto.getSector() : "");
                        resultadoEvento.put("ubicacion", dto.getUbicacion() != null ? dto.getUbicacion() : "");
                        resultadoEvento.put("urlOficial", dto.getUrlOficial() != null ? dto.getUrlOficial() : "");
                        resultadoEvento.put("fuente", dto.getFuente() != null ? dto.getFuente() : "");
                        resultadoEvento.put("guia", resultado.guia() != null ? resultado.guia() : "");
                        resultadoEvento.put("totalEncontradas", recomendaciones.size());
                        enviarEvento(emitter, "resultado", resultadoEvento);
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
