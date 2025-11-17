package com.Zygo.proyecto.controller;

import com.Zygo.proyecto.dto.PedidoDTO;
import com.Zygo.proyecto.model.Pedido.EstadoPedido;
import com.Zygo.proyecto.service.PedidoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*") // ✅ Importante para CORS
public class PedidoController {
    
    private static final Logger log = LoggerFactory.getLogger(PedidoController.class);
    
    @Autowired
    private PedidoService pedidoService;
    
    /**
     * ✅ CREAR NUEVO PEDIDO
     */
    @PostMapping
    public ResponseEntity<PedidoDTO> crearPedido(@Valid @RequestBody PedidoDTO pedidoDTO) {
        log.info("🔥 POST /api/pedidos - Creando pedido");
        log.info("📝 Datos recibidos: {}", pedidoDTO);
        log.info("📍 Coordenadas recibidas - Origen: ({}, {}), Destino: ({}, {})", 
                pedidoDTO.getLatOrigen(), pedidoDTO.getLonOrigen(),
                pedidoDTO.getLatDestino(), pedidoDTO.getLonDestino());
        
        PedidoDTO creado = pedidoService.crearPedido(pedidoDTO);
        
        log.info("✅ Pedido creado exitosamente con ID: {}", creado.getId());
        log.info("📦 Pedido completo: {}", creado);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }
    
    /**
     * ✅ OBTENER PEDIDO POR ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> obtenerPedido(@PathVariable Long id) {
        log.info("📋 GET /api/pedidos/{} - Obteniendo pedido", id);
        PedidoDTO pedido = pedidoService.obtenerPedidoPorId(id);
        return ResponseEntity.ok(pedido);
    }
    
    /**
     * ✅ LISTAR TODOS LOS PEDIDOS
     */
    @GetMapping
    public ResponseEntity<List<PedidoDTO>> obtenerTodosLosPedidos() {
        log.info("📋 GET /api/pedidos - Obteniendo todos los pedidos");
        
        List<PedidoDTO> pedidos = pedidoService.obtenerTodosLosPedidos();
        
        log.info("✅ Retornando {} pedidos", pedidos.size());
        
        // ✅ Log de IDs para debugging
        if (!pedidos.isEmpty()) {
            List<Long> ids = pedidos.stream()
                .map(PedidoDTO::getId)
                .collect(Collectors.toList());
            log.info("🔢 IDs de pedidos: {}", ids);
        }
        
        return ResponseEntity.ok(pedidos);
    }
    
    /**
     * ✅ ACTUALIZAR PEDIDO COMPLETO
     */
    @PutMapping("/{id}")
    public ResponseEntity<PedidoDTO> actualizarPedido(
            @PathVariable Long id,
            @Valid @RequestBody PedidoDTO pedidoDTO) {
        log.info("🔄 PUT /api/pedidos/{} - Actualizando pedido", id);
        PedidoDTO actualizado = pedidoService.actualizarPedido(id, pedidoDTO);
        return ResponseEntity.ok(actualizado);
    }
    
    /**
     * ✅ OBTENER PEDIDOS POR CLIENTE
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<PedidoDTO>> obtenerPedidosPorCliente(@PathVariable Long clienteId) {
        log.info("👤 GET /api/pedidos/cliente/{} - Obteniendo pedidos del cliente", clienteId);
        List<PedidoDTO> pedidos = pedidoService.obtenerPedidosPorCliente(clienteId);
        return ResponseEntity.ok(pedidos);
    }
    
    /**
     * ✅ OBTENER PEDIDOS POR REPARTIDOR
     */
    @GetMapping("/repartidor/{repartidorId}")
    public ResponseEntity<List<PedidoDTO>> obtenerPedidosPorRepartidor(@PathVariable Long repartidorId) {
        log.info("🚴 GET /api/pedidos/repartidor/{} - Obteniendo pedidos del repartidor", repartidorId);
        List<PedidoDTO> pedidos = pedidoService.obtenerPedidosPorRepartidor(repartidorId);
        return ResponseEntity.ok(pedidos);
    }
    
    /**
     * ✅ OBTENER PEDIDOS PENDIENTES
     */
    @GetMapping("/pendientes")
    public ResponseEntity<List<PedidoDTO>> obtenerPedidosPendientes() {
        log.info("⏳ GET /api/pedidos/pendientes - Obteniendo pedidos pendientes");
        List<PedidoDTO> pedidos = pedidoService.obtenerPedidosPendientes();
        return ResponseEntity.ok(pedidos);
    }
    
    /**
     * ✅ ASIGNAR REPARTIDOR
     */
    @PutMapping("/{pedidoId}/asignar/{repartidorId}")
    public ResponseEntity<PedidoDTO> asignarRepartidor(
            @PathVariable Long pedidoId,
            @PathVariable Long repartidorId) {
        log.info("👷 PUT /api/pedidos/{}/asignar/{} - Asignando repartidor", pedidoId, repartidorId);
        PedidoDTO pedido = pedidoService.asignarRepartidor(pedidoId, repartidorId);
        return ResponseEntity.ok(pedido);
    }
    
    /**
     * ✅ ACTUALIZAR ESTADO
     */
    @PutMapping("/{pedidoId}/estado")
    public ResponseEntity<PedidoDTO> actualizarEstado(
            @PathVariable Long pedidoId,
            @RequestParam EstadoPedido estado) {
        log.info("🔄 PUT /api/pedidos/{}/estado - Actualizando estado a {}", pedidoId, estado);
        PedidoDTO pedido = pedidoService.actualizarEstadoPedido(pedidoId, estado);
        return ResponseEntity.ok(pedido);
    }
    
    /**
     * ✅ MANEJO DE ERRORES
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException ex) {
        log.error("❌ Error en PedidoController: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}