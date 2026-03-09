package com.syntia.mvp.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * DTO para el formulario de creación y edición de convocatorias (panel admin).
 * Getters y setters explícitos para evitar problemas de indexación con Lombok.
 */
public class ConvocatoriaDTO {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 300, message = "El título no puede superar los 300 caracteres")
    private String titulo;

    private String tipo;
    private String sector;
    private String ubicacion;

    @Size(max = 500, message = "La URL no puede superar los 500 caracteres")
    private String urlOficial;

    private String fuente;

    /** ID interno de la BDNS — permite obtener el detalle completo de la convocatoria. */
    private String idBdns;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaCierre;

    public ConvocatoriaDTO() {}

    public String getTitulo()                   { return titulo; }
    public void   setTitulo(String titulo)      { this.titulo = titulo; }

    public String getTipo()                     { return tipo; }
    public void   setTipo(String tipo)          { this.tipo = tipo; }

    public String getSector()                   { return sector; }
    public void   setSector(String sector)      { this.sector = sector; }

    public String getUbicacion()                { return ubicacion; }
    public void   setUbicacion(String ubicacion){ this.ubicacion = ubicacion; }

    public String getUrlOficial()               { return urlOficial; }
    public void   setUrlOficial(String u)       { this.urlOficial = u; }

    public String getFuente()                   { return fuente; }
    public void   setFuente(String fuente)      { this.fuente = fuente; }

    public String getIdBdns()                   { return idBdns; }
    public void   setIdBdns(String idBdns)      { this.idBdns = idBdns; }

    public LocalDate getFechaCierre()              { return fechaCierre; }
    public void      setFechaCierre(LocalDate f)   { this.fechaCierre = f; }
}

