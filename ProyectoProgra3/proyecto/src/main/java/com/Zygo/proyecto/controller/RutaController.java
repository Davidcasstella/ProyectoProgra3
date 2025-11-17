package com.Zygo.proyecto.controller;

import com.Zygo.proyecto.dto.RutaOptimaDTO;
import com.Zygo.proyecto.model.Graph;
import com.Zygo.proyecto.model.Edge;
import com.Zygo.proyecto.service.DijkstraService;
import com.Zygo.proyecto.service.GraphManagementService;
import com.Zygo.proyecto.service.LugarService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de rutas óptimas
 */
@RestController
@RequestMapping("/api/rutas")
@CrossOrigin(origins = "*") // ✅ CORS habilitado
public class RutaController {
    
    private static final Logger log = LoggerFactory.getLogger(RutaController.class);
    
    @Autowired
    private DijkstraService dijkstraService;
    
    @Autowired
    private GraphManagementService graphManagementService;
    
    @Autowired
    private LugarService lugarService;
    
    /**
     * ✅ NUEVO: Calcula la ruta óptima usando COORDENADAS (lat, lon)
     * Este es el endpoint que necesita tu frontend
     */
    @PostMapping("/optima")
    public ResponseEntity<Map<String, Object>> calcularRutaPorCoordenadas(
            @RequestBody Map<String, Double> request) {
        
        Double latOrigen = request.get("latOrigen");
        Double lonOrigen = request.get("lonOrigen");
        Double latDestino = request.get("latDestino");
        Double lonDestino = request.get("lonDestino");
        
        log.info("🔥 POST /api/rutas/optima - Recibiendo petición");
        log.info("📍 Origen: ({}, {})", latOrigen, lonOrigen);
        log.info("🎯 Destino: ({}, {})", latDestino, lonDestino);
        
        try {
            // 1. Encontrar nodos más cercanos a las coordenadas
            log.info("🔍 Buscando nodos cercanos...");
            Graph nodoOrigen = lugarService.encontrarNodoMasCercano(latOrigen, lonOrigen);
            Graph nodoDestino = lugarService.encontrarNodoMasCercano(latDestino, lonDestino);
            
            if (nodoOrigen == null) {
                log.error("❌ No se encontró nodo cercano al origen");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No se encontró un nodo cercano al punto de origen"));
            }
            
            if (nodoDestino == null) {
                log.error("❌ No se encontró nodo cercano al destino");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No se encontró un nodo cercano al punto de destino"));
            }
            
            log.info("✅ Nodo origen encontrado: {} (ID: {})", nodoOrigen.getNombre(), nodoOrigen.getId());
            log.info("✅ Nodo destino encontrado: {} (ID: {})", nodoDestino.getNombre(), nodoDestino.getId());
            
            // 2. Calcular ruta usando Dijkstra
            log.info("🚀 Calculando ruta con Dijkstra...");
            RutaOptimaDTO ruta = dijkstraService.encontrarRutaOptima(
                nodoOrigen.getId(), 
                nodoDestino.getId(), 
                true // Considerar tráfico
            );
            
            // 3. Preparar respuesta
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("origen", nodoOrigen.getNombre());
            respuesta.put("destino", nodoDestino.getNombre());
            respuesta.put("ruta", ruta);
            
            // Agregar información adicional útil para el frontend
            Map<String, Object> camino = new HashMap<>();
            camino.put("distanciaTotal", ruta.getDistanciaTotalKm());
            camino.put("tiempoEstimado", ruta.getTiempoEstimadoMinutos());
            camino.put("nodos", ruta.getNodos());
            camino.put("instrucciones", ruta.getInstrucciones());
            respuesta.put("camino", camino);
            
            log.info("✅ Ruta calculada exitosamente");
            log.info("📊 Distancia: {} km", ruta.getDistanciaTotalKm());
            log.info("⏱️ Tiempo: {} min", ruta.getTiempoEstimadoMinutos());
            log.info("📍 Nodos en ruta: {}", ruta.getNodos().size());
            
            return ResponseEntity.ok(respuesta);
            
        } catch (Exception e) {
            log.error("❌ Error calculando ruta por coordenadas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error al calcular la ruta: " + e.getMessage()));
        }
    }
    
    /**
     * Calcula la ruta óptima entre dos NODOS (método original)
     */
    @GetMapping("/optima-por-nodos")
    public ResponseEntity<RutaOptimaDTO> calcularRutaOptimaPorNodos(
            @RequestParam Long origenId,
            @RequestParam Long destinoId,
            @RequestParam(defaultValue = "true") Boolean considerarTrafico) {
        
        log.info("GET /api/rutas/optima-por-nodos - Calculando ruta de {} a {} (tráfico: {})", 
                 origenId, destinoId, considerarTrafico);
        
        try {
            RutaOptimaDTO ruta = dijkstraService.encontrarRutaOptima(origenId, destinoId, considerarTrafico);
            return ResponseEntity.ok(ruta);
        } catch (Exception e) {
            log.error("Error calculando ruta óptima: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
    
    /**
     * Calcula rutas múltiples para varios destinos (problema del repartidor)
     */
    @PostMapping("/multiples")
    public ResponseEntity<List<RutaOptimaDTO>> calcularRutasMultiples(
            @RequestParam Long origenId,
            @RequestBody List<Long> destinosIds) {
        
        log.info("POST /api/rutas/multiples - Calculando rutas múltiples desde {} a {} destinos", 
                 origenId, destinosIds.size());
        
        try {
            List<RutaOptimaDTO> rutas = dijkstraService.encontrarRutasMultiples(origenId, destinosIds);
            return ResponseEntity.ok(rutas);
        } catch (Exception e) {
            log.error("Error calculando rutas múltiples: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
    
    /**
     * Calcula la ruta óptima para un pedido específico
     */
    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<RutaOptimaDTO> calcularRutaPedido(@PathVariable Long pedidoId) {
        log.info("GET /api/rutas/pedido/{} - Calculando ruta para pedido", pedidoId);
        
        try {
            RutaOptimaDTO ruta = graphManagementService.calcularRutaPedido(pedidoId);
            return ResponseEntity.ok(ruta);
        } catch (Exception e) {
            log.error("Error calculando ruta para pedido: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
    
    /**
     * Obtiene las rutas óptimas para todos los pedidos pendientes de un repartidor
     */
    @GetMapping("/repartidor/{repartidorId}/pendientes")
    public ResponseEntity<List<RutaOptimaDTO>> obtenerRutasRepartidor(@PathVariable Long repartidorId) {
        log.info("GET /api/rutas/repartidor/{}/pendientes - Obteniendo rutas del repartidor", repartidorId);
        
        try {
            List<RutaOptimaDTO> rutas = graphManagementService.calcularRutasRepartidor(repartidorId);
            return ResponseEntity.ok(rutas);
        } catch (Exception e) {
            log.error("Error obteniendo rutas del repartidor: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
    
    /**
     * Actualiza el factor de tráfico de una arista
     */
    @PutMapping("/trafico/{aristaId}")
    public ResponseEntity<Map<String, Object>> actualizarTrafico(
            @PathVariable Long aristaId,
            @RequestParam Double factorTrafico) {
        
        log.info("PUT /api/rutas/trafico/{} - Actualizando factor de tráfico a {}", aristaId, factorTrafico);
        
        try {
            graphManagementService.actualizarFactorTrafico(aristaId, factorTrafico);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Factor de tráfico actualizado",
                "aristaId", aristaId,
                "nuevoFactor", factorTrafico
            ));
        } catch (Exception e) {
            log.error("Error actualizando tráfico: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
    
    /**
     * Obtiene información de tráfico actual
     */
    @GetMapping("/trafico/info")
    public ResponseEntity<Map<String, Object>> obtenerInfoTrafico() {
        log.info("GET /api/rutas/trafico/info - Obteniendo información de tráfico");
        
        try {
            Map<String, Object> infoTrafico = graphManagementService.obtenerEstadoTrafico();
            return ResponseEntity.ok(infoTrafico);
        } catch (Exception e) {
            log.error("Error obteniendo información de tráfico: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * ✅ Health check para verificar que el servicio esté funcionando
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "RutaController",
            "cache", "enabled"
        ));
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException ex) {
        log.error("❌ Error en RutaController: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}