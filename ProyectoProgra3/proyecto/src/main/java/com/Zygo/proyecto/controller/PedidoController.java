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

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    
    private static final Logger log = LoggerFactory.getLogger(PedidoController.class);
    
    @Autowired
    private PedidoService pedidoService;
    
    @PostMapping
    public ResponseEntity<PedidoDTO> crearPedido(@Valid @RequestBody PedidoDTO pedidoDTO) {
        log.info("POST /api/pedidos - Creando pedido");
        PedidoDTO creado = pedidoService.crearPedido(pedidoDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> obtenerPedido(@PathVariable Long id) {
        log.info("GET /api/pedidos/{} - Obteniendo pedido", id);
        PedidoDTO pedido = pedidoService.obtenerPedidoPorId(id);
        return ResponseEntity.ok(pedido);
    }
    
    @GetMapping
    public ResponseEntity<List<PedidoDTO>> obtenerTodosLosPedidos() {
        log.info("GET /api/pedidos - Obteniendo todos los pedidos");
        List<PedidoDTO> pedidos = pedidoService.obtenerTodosLosPedidos();
        return ResponseEntity.ok(pedidos);
    }
    
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<PedidoDTO>> obtenerPedidosPorCliente(@PathVariable Long clienteId) {
        log.info("GET /api/pedidos/cliente/{} - Obteniendo pedidos del cliente", clienteId);
        List<PedidoDTO> pedidos = pedidoService.obtenerPedidosPorCliente(clienteId);
        return ResponseEntity.ok(pedidos);
    }
    
    @GetMapping("/repartidor/{repartidorId}")
    public ResponseEntity<List<PedidoDTO>> obtenerPedidosPorRepartidor(@PathVariable Long repartidorId) {
        log.info("GET /api/pedidos/repartidor/{} - Obteniendo pedidos del repartidor", repartidorId);
        List<PedidoDTO> pedidos = pedidoService.obtenerPedidosPorRepartidor(repartidorId);
        return ResponseEntity.ok(pedidos);
    }
    
    @GetMapping("/pendientes")
    public ResponseEntity<List<PedidoDTO>> obtenerPedidosPendientes() {
        log.info("GET /api/pedidos/pendientes - Obteniendo pedidos pendientes");
        List<PedidoDTO> pedidos = pedidoService.obtenerPedidosPendientes();
        return ResponseEntity.ok(pedidos);
    }
    
    @PutMapping("/{pedidoId}/asignar/{repartidorId}")
    public ResponseEntity<PedidoDTO> asignarRepartidor(
            @PathVariable Long pedidoId,
            @PathVariable Long repartidorId) {
        log.info("PUT /api/pedidos/{}/asignar/{} - Asignando repartidor", pedidoId, repartidorId);
        PedidoDTO pedido = pedidoService.asignarRepartidor(pedidoId, repartidorId);
        return ResponseEntity.ok(pedido);
    }
    
    @PutMapping("/{pedidoId}/estado")
    public ResponseEntity<PedidoDTO> actualizarEstado(
            @PathVariable Long pedidoId,
            @RequestParam EstadoPedido estado) {
        log.info("PUT /api/pedidos/{}/estado - Actualizando estado a {}", pedidoId, estado);
        PedidoDTO pedido = pedidoService.actualizarEstadoPedido(pedidoId, estado);
        return ResponseEntity.ok(pedido);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleException(RuntimeException ex) {
        log.error("Error en PedidoController: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}