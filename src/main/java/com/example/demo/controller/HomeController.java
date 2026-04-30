package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador para la página de inicio
 * Redirija / a /login.html
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "forward:/login.html";
    }
}
