package com.syntia.mvp.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * DTO que modela la guía completa de solicitud de una subvención,
 * generada por OpenAI siguiendo el esquema visual de galería interactiva.
 * <p>
 * Estructura JSON devuelta por la IA con fases: resumen, métodos de solicitud,
 * documentos, requisitos universales LGS art. 13, workflows por método,
 * guías visuales con prompts para imágenes y disclaimer legal.
 * <p>
 * Referencia normativa: Ley 38/2003 General de Subvenciones, RD 887/2006,
 * Ley 39/2015 LPACAP.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GuiaSubvencionDTO {

    @JsonProperty("grant_summary")
    private GrantSummary grantSummary;

    @JsonProperty("application_methods")
    private List<ApplicationMethod> applicationMethods;

    @JsonProperty("required_documents")
    private List<String> requiredDocuments;

    @JsonProperty("universal_requirements_lgs_art13")
    private List<String> universalRequirementsLgsArt13;

    private List<Workflow> workflows;

    @JsonProperty("visual_guides")
    private List<VisualGuide> visualGuides;

    @JsonProperty("legal_disclaimer")
    private String legalDisclaimer;

    // ── Clases internas ──

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GrantSummary {
        private String title;
        private String organism;
        private String objective;

        @JsonProperty("who_can_apply")
        private String whoCanApply;

        private String deadline;

        @JsonProperty("official_link")
        private String officialLink;

        @JsonProperty("legal_basis")
        private String legalBasis;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApplicationMethod {
        private String method;
        private String description;

        @JsonProperty("official_portal")
        private String officialPortal;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Workflow {
        private String method;
        private List<WorkflowStep> steps;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkflowStep {
        private int step;
        private String phase;
        private String title;
        private String description;

        @JsonProperty("user_action")
        private String userAction;

        @JsonProperty("portal_action")
        private String portalAction;

        @JsonProperty("required_documents")
        private List<String> requiredDocuments;

        @JsonProperty("official_link")
        private String officialLink;

        @JsonProperty("estimated_time_minutes")
        private int estimatedTimeMinutes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VisualGuide {
        private String method;
        private List<VisualStep> steps;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VisualStep {
        private int step;
        private String phase;
        private String title;
        private String description;

        @JsonProperty("screen_hint")
        private String screenHint;

        @JsonProperty("image_prompt")
        private String imagePrompt;

        @JsonProperty("official_link")
        private String officialLink;
    }
}

