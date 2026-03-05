package com.syntia.mvp.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Entidad que representa un proyecto descrito por un usuario.
 * Sirve como base para el motor de matching con convocatorias.
 */
@Entity
@Table(name = "proyectos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @NotBlank(message = "El nombre del proyecto es obligatorio")
    @Column(nullable = false)
    private String nombre;

    private String sector;

    private String ubicacion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;
}

