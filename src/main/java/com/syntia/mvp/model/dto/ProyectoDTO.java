package com.syntia.mvp.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para el formulario de creación y edición de proyectos.
 * Desacopla la entidad {@link com.syntia.mvp.model.Proyecto} de la capa de presentación.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProyectoDTO {

    @NotBlank(message = "El nombre del proyecto es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    private String sector;

    private String ubicacion;

    @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres")
    private String descripcion;
}

