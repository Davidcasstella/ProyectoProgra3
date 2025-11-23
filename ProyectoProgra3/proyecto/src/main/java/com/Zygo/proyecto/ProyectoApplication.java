package com.Zygo.proyecto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;

/**
 * 🚀 APLICACIÓN PRINCIPAL - ZYGO DELIVERY
 */
@SpringBootApplication
@EnableAsync
@EnableCaching
@OpenAPIDefinition(
    info = @Info(
        title = "Zygo Delivery API",
        version = "1.0",
        description = "Sistema de delivery con bicicletas - API REST completa",
        contact = @Contact(
            name = "Equipo Zygo",
            email = "admin@zygo.com"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Servidor Local"),
        @Server(url = "https://api.zygo.com", description = "Producción")
    },
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    description = "Ingresa el token JWT obtenido del login. Formato: Bearer {token}"
)
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
			║    📚 Swagger: http://localhost:8080/swagger-ui.html  ║
			║                                                        ║
			╚════════════════════════════════════════════════════════╝
			""");
	}
}