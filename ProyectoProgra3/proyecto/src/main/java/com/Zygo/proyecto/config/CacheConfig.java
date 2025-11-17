package com.Zygo.proyecto.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * ✅ Configuración de Cache para optimizar el rendimiento
 * Usa Caffeine (mejor que Ehcache para Spring Boot 3)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 🗺️ Cache principal para datos del mapa (nodos, aristas y rutas)
     * - Duración: 1 hora (los mapas no cambian frecuentemente)
     * - Tamaño máximo: 10,000 entradas
     * 
     * @Primary indica que este es el CacheManager por defecto
     */
    @Bean
    @Primary  // ✅ SOLUCIÓN: Marca este como el bean principal
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "grafos",           // Cache para el grafo completo
            "nodos",            // Cache para nodos individuales
            "aristas",          // Cache para aristas
            "rutas-calculadas", // Cache para rutas ya calculadas
            "rutas-optimizadas", // Cache para rutas optimizadas (más tiempo)
            "pedidos"           // Cache para pedidos
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10_000)                 // Máximo 10,000 entradas
            .expireAfterWrite(1, TimeUnit.HOURS) // Expira después de 1 hora
            .recordStats());                     // Habilitar estadísticas
        
        return cacheManager;
    }
}