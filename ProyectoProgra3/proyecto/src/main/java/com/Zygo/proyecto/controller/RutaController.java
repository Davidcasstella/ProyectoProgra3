package com.Zygo.proyecto.controller;

import com.Zygo.proyecto.dto.RutaOptimaDTO;
import com.Zygo.proyecto.service.DijkstraServiceOptimizado;
import com.Zygo.proyecto.service.LugarService;
import com.Zygo.proyecto.service.RutaService;
import com.Zygo.proyecto.model.Graph;
import com.Zygo.proyecto.model.Ruta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 📚 SWAGGER IMPORTS
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rutas")
@CrossOrigin(origins = "*")
@Tag(
    name = "3. 🗺️ Rutas", 
    description = "Cálculo de rutas óptimas usando algoritmo de Dijkstra optimizado con caché. " +
                  "Incluye cálculo por coordenadas GPS, manejo de tráfico y persistencia de rutas."
)
public class RutaController {
    
    private static final Logger log = LoggerFactory.getLogger(RutaController.class);
    
    @Autowired
    private DijkstraServiceOptimizado dijkstraService;
    
    @Autowired
    private LugarService lugarService;
    
    @Autowired
    private RutaService rutaService;
    
    @Operation(
        summary = "🎯 Calcular ruta óptima por coordenadas GPS",
        description = """
            Endpoint principal para cálculo de rutas. Características:
            - Usa algoritmo de Dijkstra optimizado con caché
            - Encuentra automáticamente los nodos más cercanos a las coordenadas
            - Considera tráfico en tiempo real (opcional)
            - Retorna instrucciones paso a paso
            - Incluye estimación de tiempo y costo
            
            Perfecto para apps móviles con GPS.
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Coordenadas de origen y destino",
            required = true,
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "latOrigen": 5.7150,
                          "lonOrigen": -72.9350,
                          "latDestino": 5.7180,
                          "lonDestino": -72.9320
                        }
                        """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Ruta calculada exitosamente - Incluye todos los nodos, segmentos e instrucciones",
            content = @Content(schema = @Schema(implementation = RutaOptimaDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Coordenadas inválidas o no hay nodos cercanos"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "No existe ruta entre los puntos especificados"
        )
    })
    @PostMapping("/optima")
    public ResponseEntity<Map<String, Object>> calcularRutaPorCoordenadas(
            @RequestBody Map<String, Double> request) {
        
        long tiempoInicio = System.currentTimeMillis();
        
        Double latOrigen = request.get("latOrigen");
        Double lonOrigen = request.get("lonOrigen");
        Double latDestino = request.get("latDestino");
        Double lonDestino = request.get("lonDestino");
        
        log.info("🚀 POST /api/rutas/optima");
        log.info("📍 Origen: ({}, {})", latOrigen, lonOrigen);
        log.info("🎯 Destino: ({}, {})", latDestino, lonDestino);
        
        try {
            // 1. Encontrar nodos más cercanos
            Graph nodoOrigen = lugarService.encontrarNodoMasCercano(latOrigen, lonOrigen);
            Graph nodoDestino = lugarService.encontrarNodoMasCercano(latDestino, lonDestino);
            
            if (nodoOrigen == null || nodoDestino == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No se encontraron nodos cercanos"));
            }
            
            log.info("✅ Nodos: {} → {}", nodoOrigen.getNombre(), nodoDestino.getNombre());
            
            // 2. Calcular ruta (CON CACHE)
            RutaOptimaDTO ruta = dijkstraService.encontrarRutaOptima(
                nodoOrigen.getId(), 
                nodoDestino.getId(), 
                true
            );
            
            // 3. Respuesta enriquecida
            long duracion = System.currentTimeMillis() - tiempoInicio;
            
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("origen", nodoOrigen.getNombre());
            respuesta.put("destino", nodoDestino.getNombre());
            respuesta.put("ruta", ruta);
            respuesta.put("tiempoCalculoMs", duracion);
            respuesta.put("cacheado", duracion < 50); // Si fue muy rápido, probablemente vino del caché
            
            Map<String, Object> resumen = new HashMap<>();
            resumen.put("distanciaTotal", ruta.getDistanciaTotalKm());
            resumen.put("tiempoEstimado", ruta.getTiempoEstimadoMinutos());
            resumen.put("numeroNodos", ruta.getNodos().size());
            resumen.put("numeroSegmentos", ruta.getSegmentos().size());
            resumen.put("costoEstimado", ruta.getCostoEstimado());
            respuesta.put("resumen", resumen);
            
            log.info("✅ Ruta calculada en {}ms - {}km, {}min", 
                     duracion, ruta.getDistanciaTotalKm(), ruta.getTiempoEstimadoMinutos());
            
            return ResponseEntity.ok(respuesta);
            
        } catch (Exception e) {
            log.error("❌ Error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    @Operation(
        summary = "💾 Guardar ruta en base de datos",
        description = """
            Persiste una ruta calculada en la base de datos para histórico y análisis.
            Útil para tracking de entregas y optimización de rutas frecuentes.
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Datos completos de la ruta a guardar",
            required = true,
            content = @Content(
                examples = @ExampleObject(
                    value = """
                        {
                          "origenLatitud": 5.7150,
                          "origenLongitud": -72.9350,
                          "destinoLatitud": 5.7180,
                          "destinoLongitud": -72.9320,
                          "distanciaKm": 3.5,
                          "tiempoEstimadoMinutos": 15,
                          "costoEstimado": 12000.0
                        }
                        """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Ruta guardada exitosamente",
            content = @Content(schema = @Schema(implementation = Ruta.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Error al guardar en base de datos"
        )
    })
    @PostMapping("/guardar")
    public ResponseEntity<Map<String, Object>> guardarRuta(
            @RequestBody Map<String, Object> rutaData) {
        
        long tiempoInicio = System.currentTimeMillis();
        
        log.info("💾 POST /api/rutas/guardar");
        log.info("📍 Origen: ({}, {})", 
            rutaData.get("origenLatitud"), 
            rutaData.get("origenLongitud"));
        log.info("🎯 Destino: ({}, {})", 
            rutaData.get("destinoLatitud"), 
            rutaData.get("destinoLongitud"));
        
        try {
            Ruta rutaGuardada = rutaService.guardarRuta(rutaData);
            
            long duracion = System.currentTimeMillis() - tiempoInicio;
            
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("id", rutaGuardada.getId());
            respuesta.put("distanciaKm", rutaGuardada.getDistanciaKm());
            respuesta.put("tiempoEstimadoMinutos", rutaGuardada.getTiempoEstimadoMinutos());
            respuesta.put("costoEstimado", rutaGuardada.getCostoEstimado());
            respuesta.put("mensaje", "Ruta guardada exitosamente");
            respuesta.put("tiempoGuardoMs", duracion);
            
            log.info("✅ Ruta guardada exitosamente en {}ms", duracion);
            
            return ResponseEntity.ok(respuesta);
            
        } catch (Exception e) {
            log.error("❌ Error al guardar ruta: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error al guardar la ruta");
            error.put("detalle", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @Operation(
        summary = "🏥 Health check del servicio de rutas",
        description = "Verifica el estado del servicio, caché y versión del algoritmo optimizado"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "Servicio operando correctamente"
        )
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "RutaController-Optimizado");
        health.put("cache", "enabled");
        health.put("algoritmo", "Dijkstra Optimizado");
        health.put("version", "2.0-optimized");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }
}