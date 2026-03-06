package com.syntia.mvp.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * DTO de lectura para mostrar datos de una Recomendacion en las vistas.
 * Evita exponer entidades JPA directamente a Thymeleaf y resuelve
 * posibles LazyInitializationException al acceder a relaciones.
 */
@Getter
@Setter
@NoArgsConstructor
public class RecomendacionDTO {

    private Long id;
    private int puntuacion;
    private String explicacion;

    /** Indica si la explicación y puntuación fueron generadas por OpenAI (true) o por el motor rule-based (false). */
    private boolean usadaIa;

    // Datos de la convocatoria asociada (desnormalizados para la vista)
    private Long convocatoriaId;
    private String titulo;
    private String tipo;
    private String sector;
    private String ubicacion;
    private String urlOficial;
    private String fuente;
    private LocalDate fechaCierre;
}

