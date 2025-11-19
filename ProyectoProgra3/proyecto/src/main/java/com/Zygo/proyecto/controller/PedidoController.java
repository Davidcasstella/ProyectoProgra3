package com.Zygo.proyecto.controller;

import com.Zygo.proyecto.dto.PedidoDTO;
import com.Zygo.proyecto.model.Pedido.EstadoPedido;
import com.Zygo.proyecto.service.PedidoService;
import com.Zygo.proyecto.service.AsignacionService;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {
    
    private static final Logger log = LoggerFactory.getLogger(PedidoController.class);
    
    @Autowired
    private PedidoService pedidoService;
    
    @Autowired
    private AsignacionService asignacionService;
    
    /**
     * 🎯 CREAR NUEVO PEDIDO CON ASIGNACIÓN AUTOMÁTICA
     * 
     * Este es el endpoint principal para clientes.
     * Acepta coordenadas GPS y busca automáticamente:
     * - Restaurante más cercano
     * - Repartidor disponible más cercano
     * - Calcula ruta óptima
     * 
     * Ejemplo:
     * POST /api/pedidos/crear-con-asignacion
     * {
     *   "clienteId": 1,
     *   "descripcion": "Pizza mediana + bebida",
     *   "direccionOrigen": "Calle 10 #5-20",
     *   "direccionDestino": "Carrera 3 #8-15",
     *   "latOrigen": 5.73,
     *   "lonOrigen": -72.93,
     *   "latDestino": 5.74,
     *   "lonDestino": -72.92,
     *   "distanciaKm": 1.5
     * }
     */
    @PostMapping("/crear-con-asignacion")
    public ResponseEntity<Map<String, Object>> crearPedidoConAsignacion(
            @Valid @RequestBody PedidoDTO pedidoDTO) {
        
        log.info("🎯 POST /api/pedidos/crear-con-asignacion");
        log.info("📦 Cliente ID: {}", pedidoDTO.getClienteId());
        log.info("📍 Ubicaciones: Origen({}, {}) → Destino({}, {})",
                pedidoDTO.getLatOrigen(), pedidoDTO.getLonOrigen(),
                pedidoDTO.getLatDestino(), pedidoDTO.getLonDestino());
        
        try {
            long tiempoInicio = System.currentTimeMillis();
            
            // PASO 1: Crear pedido básico
            PedidoDTO pedidoCreado = pedidoService.crearPedido(pedidoDTO);
            log.info("✅ Pedido creado con ID: {}", pedidoCreado.getId());
            
            // PASO 2: Activar asignación automática
            log.info("🔄 Iniciando asignación automática...");
            asignacionService.asignarPedidoAutomatico(pedidoCreado.getId());
            
            long duracion = System.currentTimeMillis() - tiempoInicio;
            
            // PASO 3: Respuesta enriquecida
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("pedido", pedidoCreado);
            respuesta.put("estado", "ASIGNACIÓN_EN_PROGRESO");
            respuesta.put("mensaje", "El pedido se está procesando automáticamente");
            respuesta.put("tiempoMs", duracion);
            
            Map<String, Object> siguientosPasos = new HashMap<>();
            siguientosPasos.put("1", "Sistema busca restaurante más cercano");
            siguientosPasos.put("2", "Sistema busca repartidor disponible");
            siguientosPasos.put("3", "Se calcula ruta óptima");
            siguientosPasos.put("4", "Repartidor recibe notificación");
            respuesta.put("siguientosPasos", siguientosPasos);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
            
        } catch (Exception e) {
            log.error("❌ Error creando pedido con asignación: {}", e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("estado", "ERROR");
            error.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * 📊 ESTADÍSTICAS DE ASIGNACIÓN
     * 
     * Muestra estado de restaurantes y repartidores
     */
    @GetMapping("/estadisticas/asignacion")
    public ResponseEntity<Map<String, Object>> obtenerEstadisticasAsignacion() {
        log.info("📊 GET /api/pedidos/estadisticas/asignacion");
        
        Map<String, Object> estadisticas = new HashMap<>();
        
        try {
            Map<String, Object> restaurantes = asignacionService.obtenerEstadisticasRestaurantes();
            Map<String, Object> repartidores = asignacionService.obtenerEstadisticasRepartidores();
            
            estadisticas.put("restaurantes", restaurantes);
            estadisticas.put("repartidores", repartidores);
            estadisticas.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(estadisticas);
            
        } catch (Exception e) {
            log.error("❌ Error obteniendo estadísticas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * ✅ OBTENER PEDIDO POR ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> obtenerPedido(@PathVariable Long id) {
        log.info("📋 GET /api/pedidos/{} - Obteniendo pedido", id);
        try {
            PedidoDTO pedido = pedidoService.obtenerPedidoPorId(id);
            return ResponseEntity.ok(pedido);
        } catch (Exception e) {
            log.error("❌ Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    /**
     * ✅ LISTAR TODOS LOS PEDIDOS
     */
    @GetMapping
    public ResponseEntity<List<PedidoDTO>> obtenerTodosLosPedidos() {
        log.info("📋 GET /api/pedidos - Obteniendo todos los pedidos");
        List<PedidoDTO> pedidos = pedidoService.obtenerTodosLosPedidos();
        log.info("✅ Total pedidos: {}", pedidos.size());
        return ResponseEntity.ok(pedidos);
    }
    
    /**
     * 📍 HISTORIAL CON COORDENADAS
     */
    @GetMapping("/historial")
    public ResponseEntity<List<PedidoDTO>> obtenerHistorial(
            @RequestParam(required = false) EstadoPedido estado) {
        log.info("📜 GET /api/pedidos/historial (estado: {})", estado);
        
        List<PedidoDTO> historial = pedidoService.obtenerTodosLosPedidos();
        
        if (estado != null) {
            historial = historial.stream()
                    .filter(p -> p.getEstado() == estado)
                    .collect(Collectors.toList());
        }
        
        historial = historial.stream()
                .sorted((p1, p2) -> p2.getFechaCreacion().compareTo(p1.getFechaCreacion()))
                .collect(Collectors.toList());
        
        log.info("✅ Historial: {} pedidos", historial.size());
        return ResponseEntity.ok(historial);
    }
    
    /**
     * ✅ ACTUALIZAR PEDIDO COMPLETO
     */
    @PutMapping("/{id}")
    public ResponseEntity<PedidoDTO> actualizarPedido(
            @PathVariable Long id,
            @Valid @RequestBody PedidoDTO pedidoDTO) {
        log.info("✏️ PUT /api/pedidos/{} - Actualizando pedido", id);
        try {
            PedidoDTO actualizado = pedidoService.actualizarPedido(id, pedidoDTO);
            return ResponseEntity.ok(actualizado);
        } catch (Exception e) {
            log.error("❌ Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * ✅ OBTENER PEDIDOS POR CLIENTE
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<PedidoDTO>> obtenerPedidosPorCliente(@PathVariable Long clienteId) {
        log.info("👤 GET /api/pedidos/cliente/{}", clienteId);
        List<PedidoDTO> pedidos = pedidoService.obtenerPedidosPorCliente(clienteId);
        return ResponseEntity.ok(pedidos);
    }
    
    /**
     * ✅ OBTENER PEDIDOS POR REPARTIDOR
     */
    @GetMapping("/repartidor/{repartidorId}")
    public ResponseEntity<List<PedidoDTO>> obtenerPedidosPorRepartidor(@PathVariable Long repartidorId) {
        log.info("🚴 GET /api/pedidos/repartidor/{}", repartidorId);
        List<PedidoDTO> pedidos = pedidoService.obtenerPedidosPorRepartidor(repartidorId);
        log.info("✅ Pedidos del repartidor: {}", pedidos.size());
        return ResponseEntity.ok(pedidos);
    }
    
    /**
     * ✅ OBTENER PEDIDOS PENDIENTES
     */
    @GetMapping("/pendientes")
    public ResponseEntity<List<PedidoDTO>> obtenerPedidosPendientes() {
        log.info("⏳ GET /api/pedidos/pendientes");
        List<PedidoDTO> pedidos = pedidoService.obtenerPedidosPendientes();
        return ResponseEntity.ok(pedidos);
    }
    
    /**
     * ✅ ASIGNAR REPARTIDOR MANUAL
     */
    @PutMapping("/{pedidoId}/asignar/{repartidorId}")
    public ResponseEntity<PedidoDTO> asignarRepartidor(
            @PathVariable Long pedidoId,
            @PathVariable Long repartidorId) {
        log.info("🚴 PUT /api/pedidos/{}/asignar/{}", pedidoId, repartidorId);
        try {
            PedidoDTO pedido = pedidoService.asignarRepartidor(pedidoId, repartidorId);
            return ResponseEntity.ok(pedido);
        } catch (Exception e) {
            log.error("❌ Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * ✅ ACTUALIZAR ESTADO
     */
    @PutMapping("/{pedidoId}/estado")
    public ResponseEntity<PedidoDTO> actualizarEstado(
            @PathVariable Long pedidoId,
            @RequestParam EstadoPedido estado) {
        log.info("📌 PUT /api/pedidos/{}/estado -> {}", pedidoId, estado);
        try {
            PedidoDTO pedido = pedidoService.actualizarEstadoPedido(pedidoId, estado);
            return ResponseEntity.ok(pedido);
        } catch (Exception e) {
            log.error("❌ Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * 🔍 BUSCAR PEDIDOS (con filtros avanzados)
     */
    @GetMapping("/buscar")
    public ResponseEntity<Map<String, Object>> buscarPedidos(
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long repartidorId) {
        
        log.info("🔍 GET /api/pedidos/buscar (estado: {}, cliente: {}, repartidor: {})",
                estado, clienteId, repartidorId);
        
        List<PedidoDTO> pedidos = pedidoService.obtenerTodosLosPedidos();
        
        if (estado != null) {
            pedidos = pedidos.stream().filter(p -> p.getEstado() == estado).collect(Collectors.toList());
        }
        if (clienteId != null) {
            pedidos = pedidos.stream().filter(p -> p.getClienteId().equals(clienteId)).collect(Collectors.toList());
        }
        if (repartidorId != null) {
            pedidos = pedidos.stream().filter(p -> p.getRepartidorId() != null && p.getRepartidorId().equals(repartidorId)).collect(Collectors.toList());
        }
        
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("total", pedidos.size());
        respuesta.put("pedidos", pedidos);
        respuesta.put("filtros", Map.of("estado", estado, "cliente", clienteId, "repartidor", repartidorId));
        
        return ResponseEntity.ok(respuesta);
    }
    
    /**
     * ❌ MANEJO DE ERRORES
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleException(RuntimeException ex) {
        log.error("❌ Error: {}", ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put("error", ex.getMessage());
        error.put("timestamp", System.currentTimeMillis());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}