package com.syntia.mvp.service;

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

    private static final String SYSTEM_PROMPT =
            "Eres el motor de recomendaciones de Syntia, plataforma espangla." +
            " Evalua la compatibilidad entre un proyecto usuario y una convocatoria publica." +
            " RESPONDE Solo con este JSON: {\"puntuacion\": N, \"explicacion\": \"texto\"}." +
            " N es entero 0-100. Explicacion max 3 frases en espanol." +
            " 80-100 alta, 50-79 media, 20-49 baja, 0-19 sin compatibilidad.";

    private static final String KEYWORDS_SYSTEM_PROMPT =
            "Eres un experto en subvenciones publicas espanolas. " +
            "A partir de un proyecto y perfil de usuario, genera grupos de palabras clave " +
            "para buscar convocatorias relevantes en la Base de Datos Nacional de Subvenciones (BDNS). " +
            "RESPONDE SOLO con un JSON: {\"busquedas\": [\"keywords1\", \"keywords2\", \"keywords3\"]}. " +
            "Cada busqueda debe ser 2-4 palabras clave en espanol, sin tildes, separadas por espacios. " +
            "Genera entre 3 y 6 busquedas distintas que cubran diferentes angulos del proyecto: " +
            "sector, tipo de ayuda, ambito geografico, tipo de entidad, etc.";

    private final OpenAiClient openAiClient;

    public OpenAiMatchingService(OpenAiClient openAiClient) {
        this.openAiClient = openAiClient;
    }

    public ResultadoIA analizar(Proyecto proyecto, Perfil perfil, Convocatoria convocatoria) {
        String userPrompt = construirPrompt(proyecto, perfil, convocatoria);
        log.debug("Prompt OpenAI proy={} conv={}", proyecto.getId(), convocatoria.getId());
        String respuesta = openAiClient.chat(SYSTEM_PROMPT, userPrompt);
        return parsearRespuesta(respuesta, proyecto, convocatoria);
    }

    private String construirPrompt(Proyecto proyecto, Perfil perfil, Convocatoria convocatoria) {
        StringBuilder sb = new StringBuilder();
        sb.append("PROYECTO: ").append(Optional.ofNullable(proyecto.getNombre()).orElse("Sin nombre")).append("\n");
        sb.append("Sector: ").append(Optional.ofNullable(proyecto.getSector()).orElse("No indicado")).append("\n");
        sb.append("Ubicacion: ").append(Optional.ofNullable(proyecto.getUbicacion()).orElse("No indicada")).append("\n");
        sb.append("Descripcion: ").append(Optional.ofNullable(proyecto.getDescripcion()).orElse("Sin descripcion")).append("\n");
        if (perfil != null) {
            sb.append("Tipo entidad: ").append(Optional.ofNullable(perfil.getTipoEntidad()).orElse("No indicado")).append("\n");
            sb.append("Objetivos: ").append(Optional.ofNullable(perfil.getObjetivos()).orElse("No indicados")).append("\n");
            sb.append("Necesidades: ").append(Optional.ofNullable(perfil.getNecesidadesFinanciacion()).orElse("No indicadas")).append("\n");
            sb.append("Perfil libre: ").append(Optional.ofNullable(perfil.getDescripcionLibre()).orElse("No proporcionada")).append("\n");
        }
        sb.append("CONVOCATORIA: ").append(convocatoria.getTitulo()).append("\n");
        sb.append("Tipo: ").append(Optional.ofNullable(convocatoria.getTipo()).orElse("No especificado")).append("\n");
        sb.append("Sector conv: ").append(Optional.ofNullable(convocatoria.getSector()).orElse("Generico")).append("\n");
        sb.append("Ambito: ").append(Optional.ofNullable(convocatoria.getUbicacion()).orElse("Nacional")).append("\n");
        sb.append("Fuente: ").append(Optional.ofNullable(convocatoria.getFuente()).orElse("No especificada")).append("\n");
        return sb.toString();
    }

    private ResultadoIA parsearRespuesta(String respuesta, Proyecto proyecto, Convocatoria convocatoria) {
        try {
            String json = extraerJson(respuesta);
            int puntuacion = extraerInt(json, "puntuacion");
            String explicacion = extraerString(json, "explicacion");
            puntuacion = Math.max(0, Math.min(100, puntuacion));
            log.debug("OpenAI punt={} proy={} conv={}", puntuacion, proyecto.getId(), convocatoria.getId());
            return new ResultadoIA(puntuacion, explicacion, true);
        } catch (Exception e) {
            log.warn("Error parseando OpenAI: {}. Raw={}", e.getMessage(), respuesta);
            throw new OpenAiClient.OpenAiUnavailableException("Respuesta no parseable: " + e.getMessage());
        }
    }

    private String extraerJson(String texto) {
        int inicio = texto.indexOf('{');
        int fin = texto.lastIndexOf('}');
        if (inicio < 0 || fin < 0 || fin <= inicio) throw new IllegalArgumentException("No JSON en respuesta");
        return texto.substring(inicio, fin + 1);
    }

    private int extraerInt(String json, String campo) {
        String k = "\"" + campo + "\"";
        int idx = json.indexOf(k);
        if (idx < 0) throw new IllegalArgumentException("Campo no encontrado: " + campo);
        int col = json.indexOf(':', idx);
        StringBuilder num = new StringBuilder();
        for (int i = col + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c)) num.append(c);
            else if (num.length() > 0) break;
        }
        return Integer.parseInt(num.toString());
    }

    private String extraerString(String json, String campo) {
        String k = "\"" + campo + "\"";
        int idx = json.indexOf(k);
        if (idx < 0) throw new IllegalArgumentException("Campo no encontrado: " + campo);
        int col = json.indexOf(':', idx);
        int p1 = json.indexOf('"', col + 1);
        if (p1 < 0) throw new IllegalArgumentException("Valor no hallado: " + campo);
        int p2 = p1 + 1;
        while (p2 < json.length()) {
            if (json.charAt(p2) == '"' && json.charAt(p2 - 1) != '\\') break;
            p2++;
        }
        return json.substring(p1 + 1, p2).replace("\\\"", "\"").replace("\\n", " ").trim();
    }

    /**
     * Usa OpenAI para generar listas de palabras clave de búsqueda a partir
     * del proyecto y perfil del usuario, para buscar en las 615.000+ convocatorias de BDNS.
     *
     * @return lista de strings de búsqueda (p.ej. ["digitalizacion pyme", "subvencion tecnologia madrid"])
     */
    public List<String> generarKeywordsBusqueda(Proyecto proyecto, Perfil perfil) {
        String userPrompt = construirPromptKeywords(proyecto, perfil);
        log.info("Generando keywords de búsqueda BDNS para proyecto={}", proyecto.getId());

        try {
            String respuesta = openAiClient.chat(KEYWORDS_SYSTEM_PROMPT, userPrompt);
            return parsearKeywords(respuesta);
        } catch (Exception e) {
            log.warn("Error generando keywords con OpenAI, usando keywords básicas: {}", e.getMessage());
            return generarKeywordsBasicas(proyecto, perfil);
        }
    }

    private String construirPromptKeywords(Proyecto proyecto, Perfil perfil) {
        StringBuilder sb = new StringBuilder();
        sb.append("PROYECTO: ").append(Optional.ofNullable(proyecto.getNombre()).orElse("")).append("\n");
        sb.append("Sector: ").append(Optional.ofNullable(proyecto.getSector()).orElse("")).append("\n");
        sb.append("Ubicacion: ").append(Optional.ofNullable(proyecto.getUbicacion()).orElse("")).append("\n");
        sb.append("Descripcion: ").append(Optional.ofNullable(proyecto.getDescripcion()).orElse("")).append("\n");
        if (perfil != null) {
            sb.append("Tipo entidad: ").append(Optional.ofNullable(perfil.getTipoEntidad()).orElse("")).append("\n");
            sb.append("Objetivos: ").append(Optional.ofNullable(perfil.getObjetivos()).orElse("")).append("\n");
            sb.append("Necesidades: ").append(Optional.ofNullable(perfil.getNecesidadesFinanciacion()).orElse("")).append("\n");
        }
        return sb.toString();
    }

    private List<String> parsearKeywords(String respuesta) {
        List<String> keywords = new ArrayList<>();
        String json = extraerJson(respuesta);

        // Extraer el array "busquedas" del JSON
        int idx = json.indexOf("\"busquedas\"");
        if (idx < 0) throw new IllegalArgumentException("No se encontró 'busquedas' en respuesta");

        int arrStart = json.indexOf('[', idx);
        int arrEnd = json.indexOf(']', arrStart);
        if (arrStart < 0 || arrEnd < 0) throw new IllegalArgumentException("Array no encontrado");

        String arrContent = json.substring(arrStart + 1, arrEnd);
        for (String item : arrContent.split(",")) {
            String kw = item.replace("\"", "").trim();
            if (!kw.isBlank()) keywords.add(kw);
        }

        log.info("Keywords generadas por OpenAI: {}", keywords);
        return keywords;
    }

    private List<String> generarKeywordsBasicas(Proyecto proyecto, Perfil perfil) {
        List<String> keywords = new ArrayList<>();
        if (proyecto.getSector() != null) keywords.add("subvencion " + proyecto.getSector().toLowerCase());
        if (proyecto.getUbicacion() != null) keywords.add("ayuda " + proyecto.getUbicacion().toLowerCase());
        if (proyecto.getDescripcion() != null && proyecto.getDescripcion().length() > 10) {
            // Extraer las primeras 3 palabras largas de la descripción
            String[] words = proyecto.getDescripcion().toLowerCase().split("\\s+");
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (String w : words) {
                if (w.length() > 4 && count < 3) { sb.append(w).append(" "); count++; }
            }
            if (sb.length() > 0) keywords.add(sb.toString().trim());
        }
        if (perfil != null && perfil.getTipoEntidad() != null) {
            keywords.add("subvencion " + perfil.getTipoEntidad().toLowerCase());
        }
        log.info("Keywords básicas (fallback): {}", keywords);
        return keywords;
    }

    public record ResultadoIA(int puntuacion, String explicacion, boolean usadaIA) {}
}
