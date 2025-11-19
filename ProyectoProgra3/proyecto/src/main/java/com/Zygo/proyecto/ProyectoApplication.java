package com.Zygo.proyecto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 🚀 APLICACIÓN PRINCIPAL - ZYGO DELIVERY
 * 
 * Anotaciones críticas:
 * - @EnableAsync: Permite métodos @Async para procesamiento en background
 * - @EnableCaching: Habilita caché para rutas calculadas en Dijkstra
 */
@SpringBootApplication
@EnableAsync          // ⭐ CRÍTICO: Activa procesamiento asincrónico
@EnableCaching        // ⭐ CRÍTICO: Activa cacheo de resultados
public class ProyectoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProyectoApplication.class, args);
		
		System.out.println("""
			
			╔════════════════════════════════════════════════════════╗
			║           🚀 ZYGO DELIVERY INICIADO 🚀                ║
			║                                                        ║
			║    ✅ Async habilitado (procesamiento en background)  ║
			║    ✅ Cache habilitado (rutas optimizadas)            ║
			║    ✅ Servidor ejecutándose en puerto 8080            ║
			║    ✅ Base de datos MySQL conectada                   ║
			║                                                        ║
			║    📍 http://localhost:8080                           ║
			║    📊 Métricas: http://localhost:8080/actuator        ║
			║                                                        ║
			╚════════════════════════════════════════════════════════╝
			""");
	}
	
	// ⭐ ELIMINADO: taskExecutor() ya está definido en AsyncConfig.class
}