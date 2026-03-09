package com.syntia.mvp.service;

import com.syntia.mvp.model.Convocatoria;
import com.syntia.mvp.model.Perfil;
import com.syntia.mvp.model.Proyecto;
import com.syntia.mvp.model.Recomendacion;
import com.syntia.mvp.repository.ConvocatoriaRepository;
import com.syntia.mvp.repository.RecomendacionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor de interpretación y matching de Syntia.
 * <p>
 * Estrategia híbrida:
 * <ol>
 *   <li><b>OpenAI (primario):</b> análisis semántico del perfil + proyecto vs. convocatoria.
 *       Genera puntuación 0-100 y explicación comprensible en lenguaje natural.</li>
 *   <li><b>Rule-based (fallback):</b> scoring determinista basado en campos estructurados.
 *       Se activa automáticamente si la API de OpenAI no está configurada o falla.</li>
 * </ol>
 * <p>
 * Algoritmo rule-based (fallback, máximo 100 puntos):
 * <ul>
 *   <li>+40 pts — sector del proyecto coincide con el sector de la convocatoria</li>
 *   <li>+30 pts — ubicación del proyecto coincide con la ubicación de la convocatoria</li>
 *   <li>+20 pts — la convocatoria es de ámbito nacional</li>
 *   <li>+10 pts — la descripción del proyecto contiene palabras clave del título</li>
 * </ul>
 */
@Slf4j
@Service
public class MotorMatchingService {

    private final ConvocatoriaRepository convocatoriaRepository;
    private final RecomendacionRepository recomendacionRepository;
    private final PerfilService perfilService;
    private final OpenAiMatchingService openAiMatchingService;
    private final ConvocatoriaService convocatoriaService;

    public MotorMatchingService(ConvocatoriaRepository convocatoriaRepository,
                                RecomendacionRepository recomendacionRepository,
                                PerfilService perfilService,
                                OpenAiMatchingService openAiMatchingService,
                                ConvocatoriaService convocatoriaService) {
        this.convocatoriaRepository = convocatoriaRepository;
        this.recomendacionRepository = recomendacionRepository;
        this.perfilService = perfilService;
        this.openAiMatchingService = openAiMatchingService;
        this.convocatoriaService = convocatoriaService;
    }

    /**
     * Genera y persiste recomendaciones para un proyecto.
     * <p>
     * Estrategia de búsqueda en las 615.000+ convocatorias de la BDNS:
     * <ol>
     *   <li>OpenAI analiza el proyecto + perfil y genera keywords de búsqueda</li>
     *   <li>Busca en la API de BDNS con esas keywords → importa candidatas a la BD local</li>
     *   <li>OpenAI evalúa cada convocatoria candidata y genera puntuación + explicación</li>
     * </ol>
     *
     * @param proyecto proyecto para el que se generan recomendaciones
     * @return lista de recomendaciones persistidas, ordenadas por puntuación desc
     */
    @Transactional
    public List<Recomendacion> generarRecomendaciones(Proyecto proyecto) {
        recomendacionRepository.deleteByProyectoId(proyecto.getId());

        // 1. Cargar el perfil del usuario
        Perfil perfil = perfilService.obtenerPerfil(proyecto.getUsuario().getId()).orElse(null);

        // 2. Generar keywords de búsqueda con OpenAI y buscar en BDNS (615K convocatorias)
        List<Convocatoria> convocatorias = buscarConvocatoriasRelevantes(proyecto, perfil);
        log.info("Convocatorias candidatas para matching: {}", convocatorias.size());

        // 3. Evaluar cada convocatoria candidata con OpenAI
        List<Recomendacion> recomendaciones = new ArrayList<>();

        for (Convocatoria convocatoria : convocatorias) {
            OpenAiMatchingService.ResultadoIA resultado = evaluarConFallback(proyecto, perfil, convocatoria);

            if (resultado.puntuacion() > 0) {
                Recomendacion rec = Recomendacion.builder()
                        .proyecto(proyecto)
                        .convocatoria(convocatoria)
                        .puntuacion(resultado.puntuacion())
                        .explicacion(resultado.explicacion())
                        .usadaIa(resultado.usadaIA())
                        .build();
                recomendaciones.add(recomendacionRepository.save(rec));
            }
        }

        recomendaciones.sort((a, b) -> Integer.compare(b.getPuntuacion(), a.getPuntuacion()));

        log.info("Motor matching completado: proyecto={} candidatas={} recomendaciones={}",
                proyecto.getId(), convocatorias.size(), recomendaciones.size());

        return recomendaciones;
    }

    // ── Búsqueda inteligente en BDNS ─────────────────────────────────────────

    /**
     * Busca convocatorias relevantes en la BDNS usando keywords generadas por OpenAI.
     * Cada keyword genera una búsqueda contra las 615.000+ convocatorias de la BDNS,
     * importando hasta 50 resultados por keyword.
     */
    private List<Convocatoria> buscarConvocatoriasRelevantes(Proyecto proyecto, Perfil perfil) {
        try {
            List<String> keywords = openAiMatchingService.generarKeywordsBusqueda(proyecto, perfil);
            for (String kw : keywords) {
                try {
                    convocatoriaService.buscarEImportarDesdeBdns(kw, 1);
                } catch (Exception e) {
                    log.warn("Error buscando en BDNS con keywords '{}': {}", kw, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Error en búsqueda BDNS, usando convocatorias existentes en BD: {}", e.getMessage());
        }

        return convocatoriaRepository.findAll();
    }

    // ── Evaluación con fallback ──────────────────────────────────────────────

    /**
     * Intenta evaluar con OpenAI. Si falla, aplica el motor rule-based.
     */
    private OpenAiMatchingService.ResultadoIA evaluarConFallback(
            Proyecto proyecto, Perfil perfil, Convocatoria convocatoria) {
        try {
            return openAiMatchingService.analizar(proyecto, perfil, convocatoria);
        } catch (OpenAiClient.OpenAiUnavailableException e) {
            log.debug("OpenAI no disponible, usando motor rule-based: {}", e.getMessage());
            return evaluarRuleBase(proyecto, convocatoria);
        }
    }

    // ── Motor rule-based (fallback) ──────────────────────────────────────────

    private OpenAiMatchingService.ResultadoIA evaluarRuleBase(Proyecto proyecto, Convocatoria convocatoria) {
        int puntuacion = calcularPuntuacion(proyecto, convocatoria);
        String explicacion = puntuacion > 0
                ? generarExplicacion(proyecto, convocatoria, puntuacion)
                : "";
        return new OpenAiMatchingService.ResultadoIA(puntuacion, explicacion, false);
    }

    private int calcularPuntuacion(Proyecto proyecto, Convocatoria convocatoria) {
        int puntuacion = 0;

        // +40 pts: coincidencia de sector
        if (proyecto.getSector() != null && convocatoria.getSector() != null
                && proyecto.getSector().equalsIgnoreCase(convocatoria.getSector())) {
            puntuacion += 40;
        }

        // +30 pts: coincidencia de ubicación exacta
        if (proyecto.getUbicacion() != null && convocatoria.getUbicacion() != null
                && proyecto.getUbicacion().equalsIgnoreCase(convocatoria.getUbicacion())) {
            puntuacion += 30;
        }

        // +20 pts: convocatoria de ámbito nacional
        if (convocatoria.getUbicacion() == null
                || convocatoria.getUbicacion().isBlank()
                || convocatoria.getUbicacion().equalsIgnoreCase("Nacional")) {
            puntuacion += 20;
        }

        // +10 pts: palabras clave del título en la descripción del proyecto
        if (proyecto.getDescripcion() != null && convocatoria.getTitulo() != null
                && contieneKeywords(proyecto.getDescripcion(), convocatoria.getTitulo())) {
            puntuacion += 10;
        }

        return puntuacion;
    }

    private boolean contieneKeywords(String descripcion, String titulo) {
        String descLower = descripcion.toLowerCase();
        String[] palabras = titulo.toLowerCase().split("\\s+");
        for (String palabra : palabras) {
            if (palabra.length() > 4 && descLower.contains(palabra)) {
                return true;
            }
        }
        return false;
    }

    private String generarExplicacion(Proyecto proyecto, Convocatoria convocatoria, int puntuacion) {
        StringBuilder sb = new StringBuilder();

        if (proyecto.getSector() != null && convocatoria.getSector() != null
                && proyecto.getSector().equalsIgnoreCase(convocatoria.getSector())) {
            sb.append("El sector de tu proyecto (").append(proyecto.getSector())
              .append(") coincide con el sector de esta convocatoria. ");
        }

        if (proyecto.getUbicacion() != null && convocatoria.getUbicacion() != null
                && proyecto.getUbicacion().equalsIgnoreCase(convocatoria.getUbicacion())) {
            sb.append("La ubicación de tu proyecto (").append(proyecto.getUbicacion())
              .append(") está dentro del ámbito geográfico de esta convocatoria. ");
        }

        if (convocatoria.getUbicacion() == null
                || convocatoria.getUbicacion().isBlank()
                || convocatoria.getUbicacion().equalsIgnoreCase("Nacional")) {
            sb.append("Esta convocatoria es de ámbito nacional, accesible desde cualquier ubicación. ");
        }

        if (proyecto.getDescripcion() != null && convocatoria.getTitulo() != null
                && contieneKeywords(proyecto.getDescripcion(), convocatoria.getTitulo())) {
            sb.append("La descripción de tu proyecto contiene términos relacionados con esta convocatoria. ");
        }

        if (sb.isEmpty()) {
            sb.append("Esta convocatoria puede ser de interés según tu perfil general. ");
        }

        sb.append("Puntuación de compatibilidad: ").append(puntuacion).append("/100.");
        return sb.toString().trim();
    }
}
