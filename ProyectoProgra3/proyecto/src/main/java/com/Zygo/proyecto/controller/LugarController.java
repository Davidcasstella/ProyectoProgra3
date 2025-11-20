package com.Zygo.proyecto.controller;

import com.Zygo.proyecto.dto.RutaOptimaDTO;
import com.Zygo.proyecto.model.Graph;
import com.Zygo.proyecto.repository.GraphRepository; // ✅ NUEVO IMPORT
import com.Zygo.proyecto.service.LugarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador para buscar lugares y calcular rutas por nombre
 */
@RestController
@RequestMapping("/api/lugares")
public class LugarController {
    
    private static final Logger log = LoggerFactory.getLogger(LugarController.class);
    
    @Autowired
    private LugarService lugarService;
    
    @Autowired
    private GraphRepository graphRepository; // ✅ NUEVO: Inyectar GraphRepository
    
    /**
     * Buscar lugares/nodos por nombre
     * 
     * Ejemplo: GET /api/lugares/buscar?query=plaza
     */
    @GetMapping("/buscar")
    public ResponseEntity<?> buscarLugares(@RequestParam String query) {
        log.info("🔍 Buscando lugares con query: '{}'", query);
        
        try {
            List<Map<String, Object>> resultados = lugarService.buscarLugaresPorNombre(query);
            
            return ResponseEntity.ok(Map.of(
                "query", query,
                "resultados", resultados,
                "total", resultados.size()
            ));
            
        } catch (Exception e) {
            log.error("Error buscando lugares: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Calcular ruta óptima usando NOMBRES de lugares
     * 
     * Ejemplo: POST /api/lugares/calcular-ruta
     * Body: {
     *   "origen": "Plaza Principal",
     *   "destino": "Universidad",
     *   "considerarTrafico": true
     * }
     */
    @PostMapping("/calcular-ruta")
    public ResponseEntity<?> calcularRutaPorNombre(@RequestBody Map<String, Object> request) {
        String origen = (String) request.get("origen");
        String destino = (String) request.get("destino");
        Boolean considerarTrafico = request.containsKey("considerarTrafico") 
            ? (Boolean) request.get("considerarTrafico") 
            : true;
        
        log.info("🗺️ Calculando ruta de '{}' a '{}' (tráfico: {})", 
                 origen, destino, considerarTrafico);
        
        try {
            // Buscar los lugares
            Graph nodoOrigen = lugarService.buscarLugarMasCercano(origen);
            Graph nodoDestino = lugarService.buscarLugarMasCercano(destino);
            
            if (nodoOrigen == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                            "error", "No se encontró el lugar de origen: " + origen,
                            "sugerencia", "Intenta con: " + lugarService.obtenerSugerencias(origen)
                        ));
            }
            
            if (nodoDestino == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                            "error", "No se encontró el lugar de destino: " + destino,
                            "sugerencia", "Intenta con: " + lugarService.obtenerSugerencias(destino)
                        ));
            }
            
            // Calcular ruta
            RutaOptimaDTO ruta = lugarService.calcularRutaEntreLugares(
                nodoOrigen, nodoDestino, considerarTrafico
            );
            
            // Respuesta enriquecida
            Map<String, Object> response = new HashMap<>();
            response.put("origen", Map.of(
                "busqueda", origen,
                "encontrado", nodoOrigen.getNombre(),
                "coordenadas", Map.of(
                    "lat", nodoOrigen.getLatitud(),
                    "lon", nodoOrigen.getLongitud()
                )
            ));
            response.put("destino", Map.of(
                "busqueda", destino,
                "encontrado", nodoDestino.getNombre(),
                "coordenadas", Map.of(
                    "lat", nodoDestino.getLatitud(),
                    "lon", nodoDestino.getLongitud()
                )
            ));
            response.put("ruta", ruta);
            response.put("resumen", Map.of(
                "distanciaKm", ruta.getDistanciaTotalKm(),
                "tiempoMinutos", ruta.getTiempoEstimadoMinutos(),
                "costoEstimado", ruta.getCostoEstimado(),
                "numeroParadas", ruta.getNodos().size()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error calculando ruta: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Calcular ruta usando coordenadas directamente
     * 
     * Ejemplo: POST /api/lugares/calcular-ruta-coordenadas
     * Body: {
     *   "latOrigen": 5.715,
     *   "lonOrigen": -72.935,
     *   "latDestino": 5.720,
     *   "lonDestino": -72.925
     * }
     */
    @PostMapping("/calcular-ruta-coordenadas")
    public ResponseEntity<?> calcularRutaPorCoordenadas(@RequestBody Map<String, Double> coords) {
        log.info("📍 Calculando ruta por coordenadas");
        
        try {
            Double latOrigen = coords.get("latOrigen");
            Double lonOrigen = coords.get("lonOrigen");
            Double latDestino = coords.get("latDestino");
            Double lonDestino = coords.get("lonDestino");
            
            if (latOrigen == null || lonOrigen == null || latDestino == null || lonDestino == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Faltan coordenadas requeridas"));
            }
            
            Graph nodoOrigen = lugarService.encontrarNodoMasCercano(latOrigen, lonOrigen);
            Graph nodoDestino = lugarService.encontrarNodoMasCercano(latDestino, lonDestino);
            
            if (nodoOrigen == null || nodoDestino == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No hay nodos cercanos a las coordenadas"));
            }
            
            RutaOptimaDTO ruta = lugarService.calcularRutaEntreLugares(
                nodoOrigen, nodoDestino, true
            );
            
            return ResponseEntity.ok(Map.of(
                "ruta", ruta,
                "origen", nodoOrigen.getNombre(),
                "destino", nodoDestino.getNombre()
            ));
            
        } catch (Exception e) {
            log.error("Error calculando ruta por coordenadas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Listar todos los lugares disponibles (para autocompletar)
     */
    @GetMapping("/listar")
    public ResponseEntity<?> listarLugares(
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "50") int limite) {
        
        log.info("📋 Listando lugares (tipo: {}, límite: {})", tipo, limite);
        
        try {
            List<Map<String, Object>> lugares = lugarService.listarLugaresDisponibles(tipo, limite);
            
            return ResponseEntity.ok(Map.of(
                "lugares", lugares,
                "total", lugares.size(),
                "tipos", List.of("RESTAURANTE", "CLIENTE", "INTERSECCION", "BASE_REPARTIDORES")
            ));
            
        } catch (Exception e) {
            log.error("Error listando lugares: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Obtener lugares cercanos a una ubicación
     */
    @GetMapping("/cercanos")
    public ResponseEntity<?> lugaresCercanos(
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam(defaultValue = "1.0") Double radioKm) {
        
        log.info("🔍 Buscando lugares cercanos a {}, {} (radio: {} km)", lat, lon, radioKm);
        
        try {
            List<Map<String, Object>> lugares = lugarService.buscarLugaresCercanos(lat, lon, radioKm);
            
            return ResponseEntity.ok(Map.of(
                "centro", Map.of("lat", lat, "lon", lon),
                "radioKm", radioKm,
                "lugares", lugares,
                "total", lugares.size()
            ));
            
        } catch (Exception e) {
            log.error("Error buscando lugares cercanos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * ✅ NUEVO: Obtener todos los restaurantes
     */
    @GetMapping("/restaurantes")
    public ResponseEntity<List<Map<String, Object>>> obtenerRestaurantes() {
        log.info("🍽️ GET /api/lugares/restaurantes - Obteniendo restaurantes");
        
        try {
            List<Graph> restaurantes = graphRepository.findByTipo(Graph.TipoNodo.RESTAURANTE);
            
            List<Map<String, Object>> resultado = restaurantes.stream()
                .map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", r.getId());
                    map.put("nombre", r.getNombre());
                    map.put("latitud", r.getLatitud());
                    map.put("longitud", r.getLongitud());
                    map.put("direccion", r.getDireccionCompleta());
                    map.put("activo", r.getActivo());
                    return map;
                })
                .collect(Collectors.toList());
            
            log.info("✅ {} restaurantes encontrados", resultado.size());
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            log.error("❌ Error obteniendo restaurantes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException ex) {
        log.error("Error en LugarController: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}