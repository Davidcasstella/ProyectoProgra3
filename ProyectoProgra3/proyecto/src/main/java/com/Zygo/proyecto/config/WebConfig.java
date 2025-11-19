package com.Zygo.proyecto.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 🌐 CONFIGURACIÓN WEB - CORS GLOBAL
 * 
 * Permite peticiones desde cualquier origen durante desarrollo
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // ⭐ Permitir TODOS los orígenes (desarrollo)
                .allowedOriginPatterns("*")
                
                // ⭐ Métodos HTTP permitidos
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                
                // ⭐ Headers permitidos
                .allowedHeaders("*")
                
                // ⭐ Exponer headers en respuesta
                .exposedHeaders("Authorization", "Content-Type")
                
                // ⭐ Permitir credenciales
                .allowCredentials(true)
                
                // ⭐ Tiempo de caché del preflight
                .maxAge(3600);
    }
}