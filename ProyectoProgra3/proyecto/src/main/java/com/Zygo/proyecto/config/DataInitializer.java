package com.Zygo.proyecto.config;

import com.Zygo.proyecto.service.OSMImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class DataInitializer {
    
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    
    @Autowired
    private OSMImportService osmImportService;
    
    @Bean
    @Order(1)
    public CommandLineRunner initializeGraphData() {
        return args -> {
            try {
                log.info("========================================");
                log.info("Sistema Zygo - Verificando grafo");
                log.info("========================================");
                
                // No importar automáticamente al iniciar
                // El admin lo hará manualmente vía API
                log.info("💡 Para importar el mapa de Sogamoso:");
                log.info("   POST /api/admin/osm/importar-sogamoso");
                log.info("========================================");
                
            } catch (Exception e) {
                log.error("Error en inicialización: {}", e.getMessage(), e);
            }
        };
    }
}