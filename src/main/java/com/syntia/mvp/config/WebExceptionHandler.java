package com.syntia.mvp.config;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@ControllerAdvice(basePackages = "com.syntia.mvp.controller")
public class WebExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleValidationException(MethodArgumentNotValidException ex, Model model) {
        model.addAttribute("error", "Error de validación: " + ex.getBindingResult().getAllErrors());
        return "error/400";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleEntityNotFoundException(EntityNotFoundException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public String handleUsernameNotFoundException(UsernameNotFoundException ex, Model model) {
        model.addAttribute("error", "Usuario no encontrado");
        return "error/404";
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalStateException(IllegalStateException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "error/409";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDeniedException(AccessDeniedException ex, Model model) {
        model.addAttribute("error", "No tienes permisos para acceder a esta página.");
        return "error/403";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleResourceNotFound(NoResourceFoundException ex, Model model) {
        model.addAttribute("error", "Recurso no encontrado: " + ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model) {
        log.error("Error inesperado en controlador MVC: {}", ex.getMessage(), ex);
        model.addAttribute("error", ex.getMessage() != null ? ex.getMessage() : "Ha ocurrido un error inesperado.");
        return "error/500";
    }
}

