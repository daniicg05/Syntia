package com.syntia.mvp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syntia.mvp.model.Convocatoria;
import com.syntia.mvp.model.Perfil;
import com.syntia.mvp.model.Proyecto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class OpenAiMatchingService {

    /**
     * Prompt de evaluación: criterios explícitos para que gpt-4.1 puntúe con precisión.
     * - 90-100: sector, tipo entidad y ubicación coinciden plenamente, requisitos claramente cumplidos.
     * - 70-89: alta compatibilidad pero con algún matiz menor (sector próximo, ámbito ampliable).
     * - 50-69: compatibilidad media, el proyecto podría adaptarse pero hay diferencias relevantes.
     * - 30-49: compatibilidad baja, solo coincidencias parciales o genéricas.
     * - 0-29: incompatible o sin datos suficientes para evaluar.
     */
    private static final String SYSTEM_PROMPT =
            "Eres el motor de recomendaciones de Syntia, plataforma española de ayudas y subvenciones públicas. " +
            "Evalúa la compatibilidad real entre un proyecto/perfil de usuario y una convocatoria pública oficial. " +
            "Criterios de puntuación (0-100): " +
            "90-100 = sector, tipo de entidad y ámbito geográfico coinciden plenamente; " +
            "70-89 = alta compatibilidad con algún matiz menor; " +
            "50-69 = el proyecto podría adaptarse pero hay diferencias relevantes; " +
            "30-49 = compatibilidad baja, solo coincidencias genéricas; " +
            "0-29 = incompatible. " +
            "En la explicación (máximo 2 frases, en español): indica primero el punto fuerte principal y luego " +
            "el requisito o condición clave que el usuario debe verificar antes de solicitar. " +
            "Sé concreto: menciona el sector, tipo de entidad o ámbito específico cuando sea relevante. " +
            "RESPONDE ÚNICAMENTE con este JSON exacto: {\"puntuacion\": N, \"explicacion\": \"texto\"}";

    private static final String KEYWORDS_SYSTEM_PROMPT =
            "Eres un experto en subvenciones públicas españolas. " +
            "A partir del proyecto y perfil de usuario, genera términos de búsqueda " +
            "para encontrar convocatorias relevantes en la BDNS (Base de Datos Nacional de Subvenciones). " +
            "Reglas: cada búsqueda debe ser 2-4 palabras clave en español, " +
            "centradas en el sector, tipo de ayuda, ámbito o tipo de entidad del proyecto. " +
            "Genera entre 4 y 6 búsquedas distintas que cubran diferentes ángulos. " +
            "RESPONDE ÚNICAMENTE con este JSON: {\"busquedas\": [\"kw1\", \"kw2\", \"kw3\"]}";

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public OpenAiMatchingService(OpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
        this.objectMapper = new ObjectMapper();
    }

    public ResultadoIA analizar(Proyecto proyecto, Perfil perfil, Convocatoria convocatoria) {
        String userPrompt = construirPrompt(proyecto, perfil, convocatoria);
        log.debug("Prompt OpenAI proy={} conv={}", proyecto.getId(), convocatoria.getId());
        String respuesta = openAiClient.chat(SYSTEM_PROMPT, userPrompt);
        return parsearRespuesta(respuesta, proyecto, convocatoria);
    }

    private String construirPrompt(Proyecto proyecto, Perfil perfil, Convocatoria convocatoria) {
        StringBuilder sb = new StringBuilder();
        sb.append("PROYECTO\n");
        sb.append("Nombre: ").append(Optional.ofNullable(proyecto.getNombre()).orElse("Sin nombre")).append("\n");
        sb.append("Sector: ").append(Optional.ofNullable(proyecto.getSector()).orElse("No indicado")).append("\n");
        sb.append("Ubicación: ").append(Optional.ofNullable(proyecto.getUbicacion()).orElse("No indicada")).append("\n");
        sb.append("Descripción: ").append(Optional.ofNullable(proyecto.getDescripcion()).orElse("Sin descripción")).append("\n");
        if (perfil != null) {
            sb.append("Tipo de entidad: ").append(Optional.ofNullable(perfil.getTipoEntidad()).orElse("No indicado")).append("\n");
            sb.append("Objetivos: ").append(Optional.ofNullable(perfil.getObjetivos()).orElse("No indicados")).append("\n");
            sb.append("Necesidades financiación: ").append(Optional.ofNullable(perfil.getNecesidadesFinanciacion()).orElse("No indicadas")).append("\n");
            if (perfil.getDescripcionLibre() != null && !perfil.getDescripcionLibre().isBlank())
                sb.append("Descripción libre: ").append(perfil.getDescripcionLibre()).append("\n");
        }
        sb.append("\nCONVOCATORIA\n");
        sb.append("Título: ").append(convocatoria.getTitulo()).append("\n");
        sb.append("Tipo: ").append(Optional.ofNullable(convocatoria.getTipo()).orElse("No especificado")).append("\n");
        sb.append("Sector: ").append(Optional.ofNullable(convocatoria.getSector()).orElse("Genérico")).append("\n");
        sb.append("Ámbito: ").append(Optional.ofNullable(convocatoria.getUbicacion()).orElse("Nacional")).append("\n");
        sb.append("Organismo: ").append(Optional.ofNullable(convocatoria.getFuente()).orElse("No especificado")).append("\n");
        return sb.toString();
    }

    private ResultadoIA parsearRespuesta(String respuesta, Proyecto proyecto, Convocatoria convocatoria) {
        try {
            JsonNode node = objectMapper.readTree(respuesta);
            int puntuacion = Math.max(0, Math.min(100, node.path("puntuacion").asInt()));
            String explicacion = node.path("explicacion").asText("Sin explicación disponible.");
            log.debug("OpenAI punt={} proy={} conv={}", puntuacion, proyecto.getId(), convocatoria.getId());
            return new ResultadoIA(puntuacion, explicacion, true);
        } catch (Exception e) {
            log.warn("Error parseando OpenAI: {}. Raw={}", e.getMessage(), respuesta);
            throw new OpenAiClient.OpenAiUnavailableException("Respuesta no parseable: " + e.getMessage());
        }
    }

    // ── Keywords ─────────────────────────────────────────────────────────────

    public List<String> generarKeywordsBusqueda(Proyecto proyecto, Perfil perfil) {
        String userPrompt = construirPromptKeywords(proyecto, perfil);
        log.info("Generando keywords BDNS para proyecto={}", proyecto.getId());
        try {
            String respuesta = openAiClient.chat(KEYWORDS_SYSTEM_PROMPT, userPrompt);
            return parsearKeywords(respuesta);
        } catch (Exception e) {
            log.warn("Error generando keywords con OpenAI, usando fallback básico: {}", e.getMessage());
            return generarKeywordsBasicas(proyecto, perfil);
        }
    }

    private String construirPromptKeywords(Proyecto proyecto, Perfil perfil) {
        StringBuilder sb = new StringBuilder();
        sb.append("Nombre: ").append(Optional.ofNullable(proyecto.getNombre()).orElse("")).append("\n");
        sb.append("Sector: ").append(Optional.ofNullable(proyecto.getSector()).orElse("")).append("\n");
        sb.append("Ubicación: ").append(Optional.ofNullable(proyecto.getUbicacion()).orElse("")).append("\n");
        sb.append("Descripción: ").append(Optional.ofNullable(proyecto.getDescripcion()).orElse("")).append("\n");
        if (perfil != null) {
            sb.append("Tipo entidad: ").append(Optional.ofNullable(perfil.getTipoEntidad()).orElse("")).append("\n");
            sb.append("Objetivos: ").append(Optional.ofNullable(perfil.getObjetivos()).orElse("")).append("\n");
            sb.append("Necesidades: ").append(Optional.ofNullable(perfil.getNecesidadesFinanciacion()).orElse("")).append("\n");
        }
        return sb.toString();
    }

    private List<String> parsearKeywords(String respuesta) {
        try {
            JsonNode node = objectMapper.readTree(respuesta);
            JsonNode busquedas = node.path("busquedas");
            List<String> keywords = new ArrayList<>();
            if (busquedas.isArray()) {
                busquedas.forEach(kw -> {
                    String s = kw.asText().trim();
                    if (!s.isBlank()) keywords.add(s);
                });
            }
            log.info("Keywords generadas por OpenAI: {}", keywords);
            return keywords.isEmpty() ? generarKeywordsBasicas(null, null) : keywords;
        } catch (Exception e) {
            log.warn("Error parseando keywords: {}", e.getMessage());
            return generarKeywordsBasicas(null, null);
        }
    }

    private List<String> generarKeywordsBasicas(Proyecto proyecto, Perfil perfil) {
        List<String> keywords = new ArrayList<>();
        if (proyecto != null) {
            if (proyecto.getSector() != null) keywords.add("subvención " + proyecto.getSector().toLowerCase());
            if (proyecto.getUbicacion() != null) keywords.add("ayuda " + proyecto.getUbicacion().toLowerCase());
        }
        if (perfil != null && perfil.getTipoEntidad() != null)
            keywords.add("subvención " + perfil.getTipoEntidad().toLowerCase());
        if (keywords.isEmpty()) keywords.add("subvención empresa");
        log.info("Keywords básicas (fallback): {}", keywords);
        return keywords;
    }

    public record ResultadoIA(int puntuacion, String explicacion, boolean usadaIA) {}
}
