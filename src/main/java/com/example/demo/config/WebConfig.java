package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de recursos estáticos (CSS, JS, imágenes)
 * Asegura que los archivos estáticos se sirven directamente sin pasar por Spring Security
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Deshabilita el manejo automático de recursos para forzar nuestras rutas explícitas
        registry.setOrder(1);
        
        // Mapea /js/** a archivos en classpath:/static/js/
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(31536000) // 1 año
                .resourceChain(true);

        // Mapea /css/** a archivos en classpath:/static/css/
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(31536000)
                .resourceChain(true);

        // Mapea /img/** e /images/** a archivos en classpath:/static/img/
        registry.addResourceHandler("/img/**", "/images/**")
                .addResourceLocations("classpath:/static/img/", "classpath:/static/images/")
                .setCachePeriod(31536000)
                .resourceChain(true);

        // Mapea archivos CSS y JS individuales en raíz
        registry.addResourceHandler("/*.css", "/*.js", "/*.ico", "/*.png", "/*.jpg", "/*.webp")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(31536000)
                .resourceChain(true);

        // Mapea archivos HTML (pero NO la raíz /)
        registry.addResourceHandler("/*.html")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600) // 1 hora para HTML
                .resourceChain(true);
    }
}
