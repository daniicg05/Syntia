package com.syntia.mvp.service;

import com.syntia.mvp.model.Convocatoria;
import com.syntia.mvp.model.Proyecto;
import com.syntia.mvp.model.Recomendacion;
import com.syntia.mvp.repository.ConvocatoriaRepository;
import com.syntia.mvp.repository.RecomendacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor de interpretación y matching de Syntia.
 * <p>
 * Implementa un scoring basado en reglas (rule-based) tal como establece
 * la documentación del proyecto, que excluye explícitamente ML/IA avanzada.
 * <p>
 * Algoritmo de scoring (máximo 100 puntos):
 * <ul>
 *   <li>+40 pts — sector del proyecto coincide exactamente con el sector de la convocatoria</li>
 *   <li>+30 pts — ubicación del proyecto coincide exactamente con la ubicación de la convocatoria</li>
 *   <li>+20 pts — la convocatoria es de ámbito nacional (ubicacion == null o "Nacional")</li>
 *   <li>+10 pts — la descripción del proyecto contiene palabras clave del título de la convocatoria</li>
 * </ul>
 * Solo se incluyen recomendaciones con puntuación &gt; 0.
 * Los resultados se ordenan de mayor a menor puntuación.
 */
@Service
public class MotorMatchingService {

    private final ConvocatoriaRepository convocatoriaRepository;
    private final RecomendacionRepository recomendacionRepository;

    public MotorMatchingService(ConvocatoriaRepository convocatoriaRepository,
                                RecomendacionRepository recomendacionRepository) {
        this.convocatoriaRepository = convocatoriaRepository;
        this.recomendacionRepository = recomendacionRepository;
    }

    /**
     * Genera y persiste recomendaciones para un proyecto.
     * Elimina las recomendaciones anteriores del proyecto antes de regenerarlas,
     * garantizando que siempre están actualizadas con las convocatorias disponibles.
     *
     * @param proyecto proyecto para el que se generan recomendaciones
     * @return lista de recomendaciones persistidas, ordenadas por puntuación desc
     */
    @Transactional
    public List<Recomendacion> generarRecomendaciones(Proyecto proyecto) {
        // Eliminar recomendaciones anteriores para este proyecto
        recomendacionRepository.deleteByProyectoId(proyecto.getId());

        List<Convocatoria> todasLasConvocatorias = convocatoriaRepository.findAll();
        List<Recomendacion> recomendaciones = new ArrayList<>();

        for (Convocatoria convocatoria : todasLasConvocatorias) {
            int puntuacion = calcularPuntuacion(proyecto, convocatoria);
            if (puntuacion > 0) {
                Recomendacion rec = Recomendacion.builder()
                        .proyecto(proyecto)
                        .convocatoria(convocatoria)
                        .puntuacion(puntuacion)
                        .explicacion(generarExplicacion(proyecto, convocatoria, puntuacion))
                        .build();
                recomendaciones.add(recomendacionRepository.save(rec));
            }
        }

        // Ordenar por puntuación descendente antes de devolver
        recomendaciones.sort((a, b) -> Integer.compare(b.getPuntuacion(), a.getPuntuacion()));
        return recomendaciones;
    }

    /**
     * Calcula la puntuación de compatibilidad entre un proyecto y una convocatoria.
     * Scoring basado en reglas documentadas (sin ML ni IA avanzada).
     */
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

        // +20 pts: convocatoria de ámbito nacional (sin restricción geográfica)
        if (convocatoria.getUbicacion() == null
                || convocatoria.getUbicacion().isBlank()
                || convocatoria.getUbicacion().equalsIgnoreCase("Nacional")) {
            puntuacion += 20;
        }

        // +10 pts: palabras clave del título de la convocatoria aparecen en la descripción
        if (proyecto.getDescripcion() != null && convocatoria.getTitulo() != null
                && contieneKeywords(proyecto.getDescripcion(), convocatoria.getTitulo())) {
            puntuacion += 10;
        }

        return puntuacion;
    }

    /**
     * Comprueba si la descripción del proyecto contiene alguna palabra significativa
     * (más de 4 caracteres) del título de la convocatoria.
     */
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

    /**
     * Genera una explicación comprensible de por qué se recomienda esta convocatoria.
     * Las explicaciones son legibles y orientadas al usuario final (no técnicas).
     */
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
            sb.append("Esta convocatoria es de ámbito nacional, por lo que es accesible desde cualquier ubicación. ");
        }

        if (proyecto.getDescripcion() != null && convocatoria.getTitulo() != null
                && contieneKeywords(proyecto.getDescripcion(), convocatoria.getTitulo())) {
            sb.append("La descripción de tu proyecto contiene términos relacionados con esta convocatoria. ");
        }

        if (sb.isEmpty()) {
            sb.append("Esta convocatoria puede ser de interés según tu perfil general.");
        }

        sb.append("Puntuación de compatibilidad: ").append(puntuacion).append("/100.");
        return sb.toString().trim();
    }
}

