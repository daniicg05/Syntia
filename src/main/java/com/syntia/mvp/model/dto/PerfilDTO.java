package com.syntia.mvp.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * DTO para el formulario de creación y edición del perfil de usuario.
 * Desacopla la entidad {@link com.syntia.mvp.model.Perfil} de la capa de presentación.
 */
@Getter
@Setter
@NoArgsConstructor
public class PerfilDTO {

    @NotBlank(message = "El sector es obligatorio")
    private String sector;

    @NotBlank(message = "La ubicación es obligatoria")
    private String ubicacion;

    private String tipoEntidad;

    @Size(max = 500, message = "Los objetivos no pueden superar los 500 caracteres")
    private String objetivos;

    @Size(max = 500, message = "Las necesidades no pueden superar los 500 caracteres")
    private String necesidadesFinanciacion;

    @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
    private String descripcionLibre;
}

