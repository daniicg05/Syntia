package com.syntia.mvp.config;

/**
 * Manejador de excepciones REST adicional.
 * <p>
 * Actualmente la gestión de excepciones REST se centraliza en
 * {@link GlobalExceptionHandler} (para controladores en com.syntia.mvp.controller.api)
 * y en {@link WebExceptionHandler} (para controladores de vistas Thymeleaf).
 * <p>
 * Esta clase se mantiene como punto de extensión futuro para handlers específicos
 * que requieran un tratamiento diferenciado.
 */
public class RestExceptionHandler {
    // Reservado para extensiones futuras
}
