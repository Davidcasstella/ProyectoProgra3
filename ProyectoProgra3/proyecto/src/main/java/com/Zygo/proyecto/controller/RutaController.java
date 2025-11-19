package com.Zygo.proyecto.controller;

import com.Zygo.proyecto.dto.RutaOptimaDTO;
import com.Zygo.proyecto.service.DijkstraServiceOptimizado; // ⚡ CAMBIO
import com.Zygo.proyecto.service.LugarService;
import com.Zygo.proyecto.model.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 🚀 Controlador OPTIMIZADO para cálculo de rutas
 */
@RestController
@RequestMapping("/api/rutas")
@CrossOrigin(origins = "*")
public class RutaController {
    
    private static final Logger log = LoggerFactory.getLogger(RutaController.class);
    
    @Autowired
    private DijkstraServiceOptimizado dijkstraService; // ⚡ USA EL OPTIMIZADO
    
    @Autowired
    private LugarService lugarService;
    
    /**
     * 🎯 Calcula ruta óptima por coordenadas (OPTIMIZADO)
     */
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
            
            // 3. Respuesta
            long duracion = System.currentTimeMillis() - tiempoInicio;
            
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("origen", nodoOrigen.getNombre());
            respuesta.put("destino", nodoDestino.getNombre());
            respuesta.put("ruta", ruta);
            respuesta.put("tiempoCalculoMs", duracion); // 📊 Métricas
            
            Map<String, Object> resumen = new HashMap<>();
            resumen.put("distanciaTotal", ruta.getDistanciaTotalKm());
            resumen.put("tiempoEstimado", ruta.getTiempoEstimadoMinutos());
            resumen.put("numeroNodos", ruta.getNodos().size());
            resumen.put("numeroSegmentos", ruta.getSegmentos().size());
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
    
    /**
     * 📊 Health check con métricas
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "RutaController-Optimizado");
        health.put("cache", "enabled");
        health.put("version", "2.0-optimized");
        return ResponseEntity.ok(health);
    }
}