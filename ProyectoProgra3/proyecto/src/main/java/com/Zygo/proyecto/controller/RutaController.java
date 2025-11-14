package com.Zygo.proyecto.controller;

import com.Zygo.proyecto.dto.RutaOptimaDTO;
import com.Zygo.proyecto.model.Graph;
import com.Zygo.proyecto.model.Edge;
import com.Zygo.proyecto.service.DijkstraService;
import com.Zygo.proyecto.service.GraphManagementService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para gestión de rutas óptimas
 */
@RestController
@RequestMapping("/api/rutas")
public class RutaController {
    
    private static final Logger log = LoggerFactory.getLogger(RutaController.class);
    
    @Autowired
    private DijkstraService dijkstraService;
    
    @Autowired
    private GraphManagementService graphManagementService;
    
    /**
     * Calcula la ruta óptima entre dos puntos
     */
    @GetMapping("/optima")
    public ResponseEntity<RutaOptimaDTO> calcularRutaOptima(
            @RequestParam Long origenId,
            @RequestParam Long destinoId,
            @RequestParam(defaultValue = "true") Boolean considerarTrafico) {
        
        log.info("GET /api/rutas/optima - Calculando ruta de {} a {} (tráfico: {})", 
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
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException ex) {
        log.error("Error en RutaController: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}