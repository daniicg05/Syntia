package com.syntia.mvp.config;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice(basePackages = "com.syntia.mvp.controller")
public class WebExceptionHandler {

    
	@ExceptionHandler(MethodArgumentNotValidException.class)
	    public String handleValidationException(MethodArgumentNotValidException ex, Model model) {
	        model.addAttribute("error", "Error de validación: " + ex.getBindingResult().getAllErrors());
	    return "error/400";
	}
	
    @ExceptionHandler(UsernameNotFoundException.class)
    public String handleUsernameNotFoundException(UsernameNotFoundException ex, Model model) {
        model.addAttribute("error", "Usuario no encontrado");
        return "error/404"; // Redirige a error/404.html
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalStateException(IllegalStateException ex, Model model) {
        model.addAttribute("error", ex.getMessage()); // Mensaje personalizado
        return "error/409"; // Redirige a error/409.html (Conflicto)
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDeniedException(AccessDeniedException ex, Model model) {
        model.addAttribute("error", "No tienes permisos para acceder a esta página.");
        return "error/403"; // Redirige a error/403.html
    }
    
    @ExceptionHandler(NoResourceFoundException.class)
    public String handleResourceNotFound(NoResourceFoundException ex, Model model) {
        model.addAttribute("error", "Recurso no encontrado: " + ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model) {
        model.addAttribute("error", "Ha ocurrido un error inesperado.");
        return "error/500"; // Redirige a error/500.html
    }
}