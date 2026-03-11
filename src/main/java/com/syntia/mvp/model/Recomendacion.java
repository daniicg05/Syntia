package com.syntia.mvp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que representa una recomendación generada por el motor de matching.
 * Vincula un proyecto con una convocatoria, incluyendo puntuación y explicación.
 */
@Entity
@Table(name = "recomendaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recomendacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convocatoria_id", nullable = false)
    private Convocatoria convocatoria;

    @Column(nullable = false)
    private int puntuacion;

    @Column(columnDefinition = "TEXT")
    private String explicacion;

    /** Guía paso a paso generada por IA: requisitos, documentación y pasos en la web oficial. */
    @Column(columnDefinition = "TEXT")
    private String guia;

    /**
     * Guía enriquecida en formato JSON completo: workflows, guías visuales, documentos,
     * requisitos universales LGS y disclaimer legal. Generada bajo demanda por OpenAiGuiaService.
     */
    @Column(name = "guia_enriquecida", columnDefinition = "TEXT")
    private String guiaEnriquecida;

    /** true = generada por OpenAI, false = motor rule-based (fallback). */
    @Column(name = "usada_ia", nullable = false)
    @Builder.Default
    private boolean usadaIa = false;

    @Column(name = "generada_en", nullable = false, updatable = false)
    private LocalDateTime generadaEn;

    @PrePersist
    protected void onCreate() {
        this.generadaEn = LocalDateTime.now();
    }
}

