package com.Zygo.proyecto.controller;

import com.Zygo.proyecto.model.Pedido;
import com.Zygo.proyecto.model.Usuario;
import com.Zygo.proyecto.repository.PedidoRepository;
import com.Zygo.proyecto.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 🛒 CONTROLADOR PARA CLIENTES
 * Solo pueden:
 * - Ver SUS propios pedidos
 * - Crear nuevos pedidos
 * - Cancelar sus pedidos (solo si están PENDIENTE)
 */
@RestController
@RequestMapping("/api/cliente")
@PreAuthorize("hasRole('CLIENTE')")
public class ClienteController {
    
    private static final Logger log = LoggerFactory.getLogger(ClienteController.class);
    
    @Autowired
    private PedidoRepository pedidoRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    /**
     * 📦 Ver SOLO MIS pedidos
     */
    @GetMapping("/mis-pedidos")
    public ResponseEntity<?> obtenerMisPedidos() {
        try {
            Usuario cliente = obtenerClienteActual();
            List<Pedido> pedidos = pedidoRepository.findByClienteId(cliente.getId());
            
            log.info("📦 Cliente {} consultó sus {} pedidos", 
                    cliente.getNombre(), pedidos.size());
            return ResponseEntity.ok(pedidos);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener pedidos del cliente: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener tus pedidos");
        }
    }
    
    /**
     * 📦 Ver UN pedido MÍO específico
     */
    @GetMapping("/mis-pedidos/{id}")
    public ResponseEntity<?> obtenerMiPedidoPorId(@PathVariable Long id) {
        try {
            Usuario cliente = obtenerClienteActual();
            
            Pedido pedido = pedidoRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
            
            // 🔒 Verificar que el pedido es del cliente
            if (!pedido.getCliente().getId().equals(cliente.getId())) {
                log.warn("⚠️ Cliente {} intentó acceder al pedido {} que no le pertenece", 
                        cliente.getNombre(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("No tienes permiso para ver este pedido");
            }
            
            return ResponseEntity.ok(pedido);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener pedido: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Pedido no encontrado");
        }
    }
    
    /**
     * ➕ Crear un NUEVO pedido
     */
    @PostMapping("/crear-pedido")
    public ResponseEntity<?> crearPedido(@RequestBody PedidoRequest request) {
        try {
            Usuario cliente = obtenerClienteActual();
            
            Pedido pedido = new Pedido();
            pedido.setCliente(cliente);
            pedido.setDescripcion(request.descripcion());
            pedido.setDireccionOrigen(request.direccionOrigen());
            pedido.setDireccionDestino(request.direccionDestino());
            pedido.setDistanciaKm(request.distanciaKm());
            pedido.setCosto(request.costo());
            pedido.setEstado(Pedido.EstadoPedido.PENDIENTE);
            
            // Coordenadas si vienen
            if (request.latOrigen() != null) pedido.setLatOrigen(request.latOrigen());
            if (request.lonOrigen() != null) pedido.setLonOrigen(request.lonOrigen());
            if (request.latDestino() != null) pedido.setLatDestino(request.latDestino());
            if (request.lonDestino() != null) pedido.setLonDestino(request.lonDestino());
            
            Pedido nuevoPedido = pedidoRepository.save(pedido);
            
            log.info("✅ Cliente {} creó pedido #{}", cliente.getNombre(), nuevoPedido.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoPedido);
            
        } catch (Exception e) {
            log.error("❌ Error al crear pedido: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al crear el pedido");
        }
    }
    
    /**
     * ❌ Cancelar MI pedido (solo si está PENDIENTE)
     */
    @PutMapping("/mis-pedidos/{id}/cancelar")
    public ResponseEntity<?> cancelarMiPedido(@PathVariable Long id) {
        try {
            Usuario cliente = obtenerClienteActual();
            
            Pedido pedido = pedidoRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
            
            // 🔒 Verificar que el pedido es del cliente
            if (!pedido.getCliente().getId().equals(cliente.getId())) {
                log.warn("⚠️ Cliente {} intentó cancelar pedido {} que no le pertenece", 
                        cliente.getNombre(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("No tienes permiso para cancelar este pedido");
            }
            
            // 🔒 Solo se puede cancelar si está PENDIENTE
            if (pedido.getEstado() != Pedido.EstadoPedido.PENDIENTE) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Solo puedes cancelar pedidos en estado PENDIENTE");
            }
            
            pedido.setEstado(Pedido.EstadoPedido.CANCELADO);
            pedidoRepository.save(pedido);
            
            log.info("❌ Cliente {} canceló pedido #{}", cliente.getNombre(), id);
            return ResponseEntity.ok("Pedido cancelado correctamente");
            
        } catch (Exception e) {
            log.error("❌ Error al cancelar pedido: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al cancelar el pedido");
        }
    }
    
    /**
     * 📊 MIS estadísticas de pedidos
     */
    @GetMapping("/mis-estadisticas")
    public ResponseEntity<?> obtenerEstadisticas() {
        try {
            Usuario cliente = obtenerClienteActual();
            List<Pedido> pedidos = pedidoRepository.findByClienteId(cliente.getId());
            
            long pendientes = pedidos.stream()
                    .filter(p -> p.getEstado() == Pedido.EstadoPedido.PENDIENTE)
                    .count();
            long enCamino = pedidos.stream()
                    .filter(p -> p.getEstado() == Pedido.EstadoPedido.EN_CAMINO)
                    .count();
            long entregados = pedidos.stream()
                    .filter(p -> p.getEstado() == Pedido.EstadoPedido.ENTREGADO)
                    .count();
            long cancelados = pedidos.stream()
                    .filter(p -> p.getEstado() == Pedido.EstadoPedido.CANCELADO)
                    .count();
            
            var estadisticas = new EstadisticasCliente(
                    pedidos.size(),
                    (int) pendientes,
                    (int) enCamino,
                    (int) entregados,
                    (int) cancelados
            );
            
            return ResponseEntity.ok(estadisticas);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener estadísticas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener estadísticas");
        }
    }
    
    // === MÉTODOS AUXILIARES ===
    
    private Usuario obtenerClienteActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }
    
    // DTOs
    private record PedidoRequest(
            String descripcion,
            String direccionOrigen,
            String direccionDestino,
            Double distanciaKm,
            Double costo,
            Double latOrigen,
            Double lonOrigen,
            Double latDestino,
            Double lonDestino
    ) {}
    
    private record EstadisticasCliente(
            int totalPedidos,
            int pendientes,
            int enCamino,
            int entregados,
            int cancelados
    ) {}
}