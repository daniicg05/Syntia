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
            "Se te proporciona: el perfil y proyecto de un usuario, y los datos de una convocatoria real de la BDNS. " +
            "Cuando se incluya la sección '=== CONTENIDO OFICIAL DE LA CONVOCATORIA ===', " +
            "DEBES usarla como fuente primaria para determinar requisitos, beneficiarios y procedimiento. " +
            "Si no hay contenido oficial, infiere a partir del título y organismo.\n\n" +
            "TU TAREA tiene dos partes:\n\n" +
            "PARTE 1 - MATCHING (puntuación 0-100):\n" +
            "- 90-100: el perfil/proyecto cumple claramente los requisitos de la convocatoria.\n" +
            "- 70-89: alta compatibilidad con algún matiz a verificar.\n" +
            "- 50-69: compatible pero hay diferencias relevantes.\n" +
            "- 30-49: compatibilidad baja.\n" +
            "- 0-29: incompatible.\n" +
            "La 'explicacion': máximo 2 frases. Primera: punto fuerte de compatibilidad. " +
            "Segunda: requisito específico de la convocatoria que el usuario debe verificar.\n\n" +
            "PARTE 2 - GUÍA DE SOLICITUD (basada en el contenido oficial si está disponible):\n" +
            "5 pasos separados por | con información EXACTA y ESPECÍFICA de esta convocatoria:\n" +
            "PASO 1: Requisitos concretos de elegibilidad extraídos del contenido oficial " +
            "(tipo de beneficiario, sector, tamaño empresa, antigüedad mínima, etc.).\n" +
            "PASO 2: Documentación específica requerida según el organismo convocante " +
            "(certificados, memorias, planes, declaraciones responsables, etc.).\n" +
            "PASO 3: Cómo y dónde presentar la solicitud — indica la sede electrónica o plataforma " +
            "específica del organismo convocante mencionado.\n" +
            "PASO 4: Plazos concretos — fecha de cierre si la conoces, plazo de resolución, " +
            "inicio mínimo de actividad subvencionable.\n" +
            "PASO 5: Advertencia clave o consejo específico para esta convocatoria " +
            "(incompatibilidades, criterios de valoración, errores frecuentes).\n\n" +
            "RESPONDE ÚNICAMENTE con este JSON (sin texto adicional fuera del JSON):\n" +
            "{\"puntuacion\": N, \"explicacion\": \"texto\", \"sector\": \"UNA_PALABRA\", " +
            "\"guia\": \"PASO 1: texto|PASO 2: texto|PASO 3: texto|PASO 4: texto|PASO 5: texto\"}";

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

    public ResultadoIA analizar(Proyecto proyecto, Perfil perfil, Convocatoria convocatoria, String detalleTexto) {
        String userPrompt = construirPrompt(proyecto, perfil, convocatoria, detalleTexto);
        log.debug("Prompt OpenAI proy={} conv={} detalle={}chars",
                proyecto.getId(), convocatoria.getId(),
                detalleTexto != null ? detalleTexto.length() : 0);
        String respuesta = openAiClient.chat(SYSTEM_PROMPT, userPrompt);
        return parsearRespuesta(respuesta, proyecto, convocatoria);
    }

    private String construirPrompt(Proyecto proyecto, Perfil perfil, Convocatoria convocatoria, String detalleTexto) {
        StringBuilder sb = new StringBuilder();

        // ── CONVOCATORIA (primero para que la IA la tenga clara antes de leer el perfil) ──
        sb.append("=== CONVOCATORIA PÚBLICA (BDNS) ===\n");
        sb.append("Título completo: ").append(convocatoria.getTitulo()).append("\n");
        sb.append("Organismo convocante: ").append(Optional.ofNullable(convocatoria.getFuente()).orElse("No especificado")).append("\n");
        sb.append("Tipo de convocatoria: ").append(Optional.ofNullable(convocatoria.getTipo()).orElse("No especificado")).append("\n");
        sb.append("Ámbito geográfico: ").append(Optional.ofNullable(convocatoria.getUbicacion()).orElse("Nacional")).append("\n");
        if (convocatoria.getSector() != null && !convocatoria.getSector().isBlank())
            sb.append("Sector de la convocatoria: ").append(convocatoria.getSector()).append("\n");
        if (convocatoria.getFechaCierre() != null)
            sb.append("Fecha de cierre: ").append(convocatoria.getFechaCierre()).append("\n");
        if (convocatoria.getUrlOficial() != null && !convocatoria.getUrlOficial().isBlank())
            sb.append("URL oficial: ").append(convocatoria.getUrlOficial()).append("\n");

        // ── CONTENIDO REAL DE LA CONVOCATORIA (obtenido de la API BDNS) ──
        if (detalleTexto != null && !detalleTexto.isBlank()) {
            sb.append("\n=== CONTENIDO OFICIAL DE LA CONVOCATORIA ===\n");
            // Limitar a 1500 chars para no saturar el contexto de tokens
            String detalleTruncado = detalleTexto.length() > 1500
                    ? detalleTexto.substring(0, 1500) + "..."
                    : detalleTexto;
            sb.append(detalleTruncado).append("\n");
            sb.append("(Usa este contenido para determinar requisitos EXACTOS y generar una guía precisa.)\n");
        } else {
            sb.append("\n(Nota: detalle de la convocatoria no disponible en la API. ");
            sb.append("Infiere los requisitos a partir del título y organismo.)\n");
        }

        // ── PROYECTO DEL USUARIO ──
        sb.append("\n=== PROYECTO DEL USUARIO ===\n");
        sb.append("Nombre: ").append(Optional.ofNullable(proyecto.getNombre()).orElse("Sin nombre")).append("\n");
        sb.append("Sector: ").append(Optional.ofNullable(proyecto.getSector()).orElse("No indicado")).append("\n");
        sb.append("Ubicación: ").append(Optional.ofNullable(proyecto.getUbicacion()).orElse("No indicada")).append("\n");
        sb.append("Descripción: ").append(Optional.ofNullable(proyecto.getDescripcion()).orElse("Sin descripción")).append("\n");

        // ── PERFIL DE LA ENTIDAD ──
        if (perfil != null) {
            sb.append("\n=== PERFIL DE LA ENTIDAD ===\n");
            sb.append("Tipo de entidad: ").append(Optional.ofNullable(perfil.getTipoEntidad()).orElse("No indicado")).append("\n");
            sb.append("Sector de actividad: ").append(Optional.ofNullable(perfil.getSector()).orElse("No indicado")).append("\n");
            sb.append("Ubicación: ").append(Optional.ofNullable(perfil.getUbicacion()).orElse("No indicada")).append("\n");
            sb.append("Objetivos: ").append(Optional.ofNullable(perfil.getObjetivos()).orElse("No indicados")).append("\n");
            sb.append("Necesidades de financiación: ").append(Optional.ofNullable(perfil.getNecesidadesFinanciacion()).orElse("No indicadas")).append("\n");
            if (perfil.getDescripcionLibre() != null && !perfil.getDescripcionLibre().isBlank())
                sb.append("Descripción libre: ").append(perfil.getDescripcionLibre()).append("\n");
        }

        sb.append("\n=== INSTRUCCIÓN ===\n");
        if (detalleTexto != null) {
            sb.append("Usa el CONTENIDO OFICIAL de la convocatoria para generar requisitos y guía EXACTOS y precisos. ");
            sb.append("La guía debe basarse en los datos reales del documento oficial.");
        } else {
            sb.append("No hay contenido oficial disponible. Infiere los requisitos y guía a partir del título y organismo.");
        }

        return sb.toString();
    }

    private ResultadoIA parsearRespuesta(String respuesta, Proyecto proyecto, Convocatoria convocatoria) {
        try {
            JsonNode node = objectMapper.readTree(respuesta);
            int puntuacion = Math.max(0, Math.min(100, node.path("puntuacion").asInt()));
            String explicacion = node.path("explicacion").asText("Sin explicación disponible.");
            String sector = node.path("sector").asText(null);
            if (sector != null && sector.isBlank()) sector = null;
            String guia = node.path("guia").asText(null);
            if (guia != null && guia.isBlank()) guia = null;
            log.debug("OpenAI punt={} sector='{}' proy={} conv={}", puntuacion, sector, proyecto.getId(), convocatoria.getId());
            return new ResultadoIA(puntuacion, explicacion, sector, guia, true);
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

    public record ResultadoIA(int puntuacion, String explicacion, String sector, String guia, boolean usadaIA) {}
}
