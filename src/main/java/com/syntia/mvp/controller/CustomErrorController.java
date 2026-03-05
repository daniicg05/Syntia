
package com.syntia.mvp.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador de errores personalizado.
 * Maneja errores HTTP no capturados por {@code @ControllerAdvice}.
 */
@Controller
public class CustomErrorController implements ErrorController {

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        model.addAttribute("error", "Ha ocurrido un error inesperado");
        return "error/error";
    }
}
