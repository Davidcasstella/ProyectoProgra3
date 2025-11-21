package com.Zygo.proyecto.controller;

import com.Zygo.proyecto.dto.HistorialRutaDTO;
import com.Zygo.proyecto.dto.RutaOptimaDTO;
import com.Zygo.proyecto.model.HistorialRuta;
import com.Zygo.proyecto.model.Pedido;
import com.Zygo.proyecto.repository.HistorialRutaRepository;
import com.Zygo.proyecto.repository.PedidoRepository;
import com.Zygo.proyecto.service.HistorialRutaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 📊 CONTROLLER: Historial de Rutas
 * 
 * Endpoints para consultar y visualizar el historial de rutas calculadas
 * - Admin puede ver todo
 * - Repartidor puede ver solo sus rutas
 * - Cliente NO tiene acceso
 */
@RestController
@RequestMapping("/api/historial-rutas")
@CrossOrigin(origins = "*")
public class HistorialRutaController {
    
    private static final Logger log = LoggerFactory.getLogger(HistorialRutaController.class);
    
    @Autowired
    private HistorialRutaService historialRutaService;
    
    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
private HistorialRutaRepository historialRutaRepository;
    
    /**
     * 📋 OBTENER HISTORIAL COMPLETO (Solo Admin)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerHistorialCompleto(
            @RequestParam(defaultValue = "50") int limite) {
        
        log.info("📋 GET /api/historial-rutas (límite: {})", limite);
        
        try {
            List<HistorialRutaDTO> historial = historialRutaService.obtenerHistorialCompleto(limite);
            
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("total", historial.size());
            respuesta.put("limite", limite);
            respuesta.put("historial", historial);
            
            log.info("✅ {} registros de historial obtenidos", historial.size());
            
            return ResponseEntity.ok(respuesta);
            
        } catch (Exception e) {
            log.error("❌ Error obteniendo historial: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
   @GetMapping("/pedido/{pedidoId}")
@PreAuthorize("hasAnyRole('ADMIN', 'REPARTIDOR')")
public ResponseEntity<Map<String, Object>> obtenerHistorialPorPedido(
        @PathVariable Long pedidoId) {
    
    log.info("🔍 GET /api/historial-rutas/pedido/{}", pedidoId);
    
    try {
        // ✅ FIX: Buscar directamente del repository y convertir con nuestro método
        List<HistorialRuta> historiales = historialRutaRepository.findByPedidoIdAndActivoTrue(pedidoId);
        
        List<HistorialRutaDTO> dtos = historiales.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
        
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("pedidoId", pedidoId);
        respuesta.put("total", dtos.size());
        respuesta.put("historial", dtos);
        
        log.info("✅ {} registros encontrados para pedido {}", dtos.size(), pedidoId);
        
        return ResponseEntity.ok(respuesta);
        
    } catch (Exception e) {
        log.error("❌ Error obteniendo historial del pedido: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }
}
    /**
     * 🚴 OBTENER HISTORIAL POR REPARTIDOR
     */
    @GetMapping("/repartidor/{repartidorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPARTIDOR')")
    public ResponseEntity<Map<String, Object>> obtenerHistorialPorRepartidor(
            @PathVariable Long repartidorId) {
        
        log.info("🚴 GET /api/historial-rutas/repartidor/{}", repartidorId);
        
        try {
            List<HistorialRutaDTO> historial = historialRutaService
                    .obtenerHistorialPorRepartidor(repartidorId);
            
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("repartidorId", repartidorId);
            respuesta.put("total", historial.size());
            respuesta.put("historial", historial);
            
            log.info("✅ {} rutas encontradas para repartidor {}", historial.size(), repartidorId);
            
            return ResponseEntity.ok(respuesta);
            
        } catch (Exception e) {
            log.error("❌ Error obteniendo historial del repartidor: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 🔄 RECREAR RUTA DESDE HISTORIAL
     */
    @GetMapping("/{historialId}/recrear")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPARTIDOR')")
    public ResponseEntity<Map<String, Object>> recrearRuta(
            @PathVariable Long historialId) {
        
        log.info("🔄 GET /api/historial-rutas/{}/recrear", historialId);
        
        try {
            RutaOptimaDTO ruta = historialRutaService.recrearRutaDesdeHistorial(historialId);
            
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("historialId", historialId);
            respuesta.put("ruta", ruta);
            respuesta.put("mensaje", "Ruta recreada exitosamente");
            
            log.info("✅ Ruta recreada: {}km, {} nodos", 
                    ruta.getDistanciaTotalKm(), 
                    ruta.getNodos() != null ? ruta.getNodos().size() : 0);
            
            return ResponseEntity.ok(respuesta);
            
        } catch (Exception e) {
            log.error("❌ Error recreando ruta: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
 * 🗺️ ENDPOINT: Obtener datos para visualizar en mapa
 * 
 * FIX: NullPointerException en Map.of()
 * Solución: Usar HashMap y validar nulls
 */
@GetMapping("/{historialId}/mapa")
@PreAuthorize("hasAnyRole('ADMIN', 'REPARTIDOR')")
public ResponseEntity<Map<String, Object>> obtenerDatosParaMapa(
        @PathVariable Long historialId) {
    
    log.info("🗺️ GET /api/historial-rutas/{}/mapa", historialId);
    
    try {
        // Obtener el historial completo
        HistorialRutaDTO historial = historialRutaService
                .obtenerHistorialCompleto(100)
                .stream()
                .filter(h -> h.getId().equals(historialId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Historial no encontrado"));
        
        // Recrear la ruta
        RutaOptimaDTO ruta = historialRutaService.recrearRutaDesdeHistorial(historialId);
        
        // ✅ FIX: Usar HashMap y validar todos los valores
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("historial", historial);
        respuesta.put("ruta", ruta);
        
        // Puntos clave (validar nulls)
        Map<String, Object> puntosClave = new HashMap<>();
        if (historial.getRestaurante() != null) {
            puntosClave.put("restaurante", historial.getRestaurante());
        }
        if (historial.getNodoCliente() != null) {
            puntosClave.put("cliente", historial.getNodoCliente());
        }
        if (historial.getNodoRepartidor() != null) {
            puntosClave.put("repartidor", historial.getNodoRepartidor());
        }
        respuesta.put("puntosClave", puntosClave);
        
        log.info("✅ Datos para mapa obtenidos");
        
        return ResponseEntity.ok(respuesta);
        
    } catch (Exception e) {
        log.error("❌ Error obteniendo datos para mapa: {}", e.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Error al obtener datos del mapa");
        error.put("detalle", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
    
    /**
     * 📊 OBTENER ESTADÍSTICAS
     */
    @GetMapping("/estadisticas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas() {
        log.info("📊 GET /api/historial-rutas/estadisticas");
        
        try {
            Map<String, Object> stats = historialRutaService.obtenerEstadisticas();
            
            log.info("✅ Estadísticas generadas: {} rutas totales", stats.get("totalRutas"));
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("❌ Error obteniendo estadísticas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 🧪 ENDPOINT TEMPORAL DE PRUEBA - ELIMINAR EN PRODUCCIÓN
     * 
     * Guarda historial manualmente para verificar que todo funciona
     */
    @PostMapping("/test/guardar-manual")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> guardarHistorialManualPrueba(
            @RequestParam Long pedidoId) {
        
        log.info("🧪 POST /api/historial-rutas/test/guardar-manual (pedido: {})", pedidoId);
        
        try {
            // Buscar el pedido
            Pedido pedido = pedidoRepository.findById(pedidoId)
                    .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
            
            // Crear un DTO de ruta ficticio para la prueba
            RutaOptimaDTO rutaOptima = new RutaOptimaDTO();
            rutaOptima.setDistanciaTotalKm(5.5);
            rutaOptima.setTiempoEstimadoMinutos(25);
            rutaOptima.setCostoEstimado(15000.0);
            rutaOptima.setConsiderandoTrafico(true);
            
            // Crear nodos ficticios
            RutaOptimaDTO.NodoDTO nodo1 = new RutaOptimaDTO.NodoDTO();
            nodo1.setId(1L);
            nodo1.setNombre("Inicio");
            nodo1.setLatitud(4.6097);
            nodo1.setLongitud(-74.0817);
            
            RutaOptimaDTO.NodoDTO nodo2 = new RutaOptimaDTO.NodoDTO();
            nodo2.setId(2L);
            nodo2.setNombre("Fin");
            nodo2.setLatitud(4.6487);
            nodo2.setLongitud(-74.0628);
            
            rutaOptima.setNodos(List.of(nodo1, nodo2));
            rutaOptima.setInstrucciones(List.of(
                "Salir hacia el norte",
                "Girar a la derecha",
                "Continuar 3km",
                "Llegaste al destino"
            ));
            
            // Guardar el historial
            HistorialRuta historial = historialRutaService.guardarHistorial(
                pedidoId,
                rutaOptima,
                pedido.getRestaurante(),
                pedido.getNodoCliente(),
                pedido.getNodoRepartidor(),
                pedido.getRepartidor(),
                HistorialRuta.TipoCalculo.ASIGNACION_AUTOMATICA,
                150L // 150ms de cálculo
            );
            
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje", "✅ Historial de prueba guardado correctamente");
            respuesta.put("historialId", historial.getId());
            respuesta.put("pedidoId", pedidoId);
            respuesta.put("distanciaKm", historial.getDistanciaTotalKm());
            respuesta.put("tiempoMin", historial.getTiempoEstimadoMin());
            
            log.info("✅ Historial de prueba creado: ID {}", historial.getId());
            
            return ResponseEntity.ok(respuesta);
            
        } catch (Exception e) {
            log.error("❌ Error guardando historial de prueba: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }




// HistorialRutaController.java

@GetMapping("/{id}")
public ResponseEntity<HistorialRutaDTO> obtenerPorId(@PathVariable Long id) {
    log.info("📋 GET /api/historial-rutas/{}", id);
    
    HistorialRuta historial = historialRutaRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Historial no encontrado"));
    
    HistorialRutaDTO dto = convertirADTO(historial);
    
    return ResponseEntity.ok(dto);
}

    @GetMapping("/ultimas/{cantidad}")
    public ResponseEntity<List<HistorialRutaDTO>> obtenerUltimas(@PathVariable int cantidad) {
        log.info("📋 GET /api/historial-rutas/ultimas/{}", cantidad);
        
        Pageable pageable = PageRequest.of(0, cantidad, Sort.by("fechaCalculo").descending());
        List<HistorialRuta> historial = historialRutaRepository.findAll(pageable).getContent();
        
        List<HistorialRutaDTO> dtos = historial.stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    private HistorialRutaDTO convertirADTO(HistorialRuta historial) {
    HistorialRutaDTO dto = new HistorialRutaDTO();
    dto.setId(historial.getId());
    dto.setPedidoId(historial.getPedido().getId());
    dto.setTipoCalculo(historial.getTipoCalculo().name()); // Convertir enum a String
    dto.setFechaCalculo(historial.getFechaCalculo());
    dto.setDistanciaTotalKm(historial.getDistanciaTotalKm());
    dto.setTiempoEstimadoMin(historial.getTiempoEstimadoMin());
    
    // Relaciones - IDs simples
    if (historial.getRestaurante() != null) {
        dto.setRestauranteId(historial.getRestaurante().getId());
        dto.setRestauranteNombre(historial.getRestaurante().getNombre());
    }
    
    if (historial.getRepartidor() != null) {
        dto.setRepartidorId(historial.getRepartidor().getId());
        dto.setRepartidorNombre(historial.getRepartidor().getNombre());
    }
    
    if (historial.getNodoCliente() != null) {
        dto.setNodoClienteId(historial.getNodoCliente().getId());
    }
    
    if (historial.getNodoRepartidor() != null) {
        dto.setNodoRepartidorId(historial.getNodoRepartidor().getId());
    }
    
    // JSON como String (usa los nombres correctos de tu DTO)
    dto.setNodosRutaJson(historial.getNodosRuta());
    dto.setSegmentosRutaJson(historial.getSegmentosRuta());
    dto.setInstruccionesJson(historial.getInstrucciones());
    
    return dto;
}
}