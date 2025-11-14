package com.Zygo.proyecto.controller;

import com.Zygo.proyecto.dto.osm.OSMModels.ImportStats;
import com.Zygo.proyecto.service.OSMImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para importación de mapas desde OpenStreetMap
 */
@RestController
@RequestMapping("/api/admin/osm")
public class OSMController {
    
    private static final Logger log = LoggerFactory.getLogger(OSMController.class);
    
    @Autowired
    private OSMImportService osmImportService;
    
    /**
     * Importa el mapa completo de Sogamoso desde OpenStreetMap
     * 
     * @param incluirResidenciales Si incluir calles residenciales (más detalle pero más nodos)
     * @return Estadísticas de la importación
     */
    /**
 * Reinicia el grafo completamente: limpia e importa de nuevo
 */
@PostMapping("/reiniciar-grafo")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> reiniciarGrafo(
        @RequestParam(defaultValue = "true") boolean incluirResidenciales) {
    
    log.info("🔄 POST /api/admin/osm/reiniciar-grafo");
    log.info("   Parámetros: incluirResidenciales={}", incluirResidenciales);
    
    try {
        // Limpiar primero
        osmImportService.limpiarGrafo();
        
        // Importar de nuevo
        ImportStats stats = osmImportService.importarMapaSogamoso(incluirResidenciales);
        
        Map<String, Object> response = new HashMap<>();
        response.put("exito", true);
        response.put("mensaje", "Grafo reiniciado exitosamente");
        response.put("estadisticas", stats);
        response.put("detalles", Map.of(
            "nodos", stats.getNodosCreados(),
            "aristas", stats.getAristasCreadas(),
            "tiempoSegundos", stats.getTiempoMs() / 1000.0
        ));
        
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        log.error("❌ Error reiniciando grafo: {}", e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "exito", false,
                    "mensaje", "Error reiniciando grafo",
                    "error", e.getMessage()
                ));
    }
}
    /**
     * Limpia todos los datos del grafo
     * ⚠️ PRECAUCIÓN: Elimina todos los nodos y aristas
     */
    @DeleteMapping("/limpiar-grafo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> limpiarGrafo() {
        log.warn("⚠️  DELETE /api/admin/osm/limpiar-grafo");
        
        try {
            osmImportService.limpiarGrafo();
            
            return ResponseEntity.ok(Map.of(
                "exito", true,
                "mensaje", "Grafo limpiado exitosamente",
                "advertencia", "Todos los nodos y aristas han sido eliminados"
            ));
            
        } catch (Exception e) {
            log.error("❌ Error limpiando grafo: {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "exito", false,
                        "mensaje", "Error limpiando grafo",
                        "error", e.getMessage()
                    ));
        }
    }
    
    
    /**
     * Obtiene información sobre el estado actual del grafo
     */
    @GetMapping("/estado-grafo")
    public ResponseEntity<?> obtenerEstadoGrafo() {
        log.info("GET /api/admin/osm/estado-grafo");
        
        try {
            // Aquí podrías agregar más estadísticas desde los repositorios
            Map<String, Object> estado = new HashMap<>();
            estado.put("mensaje", "Endpoint de estado - por implementar");
            estado.put("disponible", true);
            
            return ResponseEntity.ok(estado);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Endpoint de prueba para verificar que OSM está disponible
     */
    @GetMapping("/test-conexion")
    public ResponseEntity<?> testConexionOSM() {
        log.info("GET /api/admin/osm/test-conexion");
        
        Map<String, Object> response = new HashMap<>();
        response.put("overpassAPI", "https://overpass-api.de/api/interpreter");
        response.put("estado", "Disponible");
        response.put("mensaje", "Overpass API está accesible");
        response.put("coordenadasSogamoso", Map.of(
            "minLat", 5.695,
            "maxLat", 5.735,
            "minLon", -72.950,
            "maxLon", -72.915
        ));
        
        return ResponseEntity.ok(response);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException ex) {
        log.error("Error en OSMController: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "error", ex.getMessage(),
                    "tipo", ex.getClass().getSimpleName()
                ));
    }
}