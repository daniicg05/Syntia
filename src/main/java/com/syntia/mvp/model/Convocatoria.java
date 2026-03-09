package com.syntia.mvp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entidad que representa una convocatoria pública de subvención, ayuda o licitación.
 * Los datos provienen de fuentes oficiales como la BDNS.
 */
@Entity
@Table(name = "convocatorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Convocatoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    private String tipo;

    private String sector;

    private String ubicacion;

    @Column(name = "url_oficial")
    private String urlOficial;

    private String fuente;

    @Column(name = "id_bdns")
    private String idBdns;

    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;
}

