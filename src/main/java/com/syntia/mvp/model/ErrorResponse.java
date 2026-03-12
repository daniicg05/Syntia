package com.syntia.mvp.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * DTO para respuestas de error estandarizadas en la API REST.
 */
@Getter
@Setter
@AllArgsConstructor
@ToString
public class ErrorResponse {

    private int status;
    private String message;
    private LocalDateTime timestamp;
    private String path;
}
