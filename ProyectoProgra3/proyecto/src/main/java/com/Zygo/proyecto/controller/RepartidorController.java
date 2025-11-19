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
 * 🚚 CONTROLADOR PARA REPARTIDORES
 * Solo pueden:
 * - Ver pedidos asignados a ellos
 * - Actualizar estado de sus pedidos
 * - Actualizar su ubicación
 * - Cambiar su disponibilidad
 */
@RestController
@RequestMapping("/api/repartidor")
@PreAuthorize("hasRole('REPARTIDOR')")
public class RepartidorController {
    
    private static final Logger log = LoggerFactory.getLogger(RepartidorController.class);
    
    @Autowired
    private PedidoRepository pedidoRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    /**
     * 📦 Ver SOLO mis pedidos asignados
     */
    @GetMapping("/mis-pedidos")
    public ResponseEntity<?> obtenerMisPedidosAsignados() {
        try {
            Usuario repartidor = obtenerRepartidorActual();
            List<Pedido> pedidos = pedidoRepository.findByRepartidorId(repartidor.getId());
            
            log.info("📦 Repartidor {} consultó sus {} pedidos asignados", 
                    repartidor.getNombre(), pedidos.size());
            return ResponseEntity.ok(pedidos);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener pedidos del repartidor: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener pedidos asignados");
        }
    }
    
    /**
     * 📦 Ver solo pedidos PENDIENTES asignados a mí
     */
    @GetMapping("/mis-pedidos/pendientes")
    public ResponseEntity<?> obtenerMisPedidosPendientes() {
        try {
            Usuario repartidor = obtenerRepartidorActual();
            List<Pedido> pedidos = pedidoRepository.findByRepartidorIdAndEstado(
                    repartidor.getId(), 
                    Pedido.EstadoPedido.PENDIENTE
            );
            
            return ResponseEntity.ok(pedidos);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener pedidos pendientes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener pedidos pendientes");
        }
    }
    
    /**
     * 🔄 Actualizar estado de MI pedido
     */
    @PutMapping("/mis-pedidos/{id}/estado")
    public ResponseEntity<?> actualizarEstadoPedido(
            @PathVariable Long id,
            @RequestBody EstadoRequest request) {
        try {
            Usuario repartidor = obtenerRepartidorActual();
            
            Pedido pedido = pedidoRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
            
            // 🔒 Verificar que el pedido está asignado al repartidor
            if (pedido.getRepartidor() == null || 
                !pedido.getRepartidor().getId().equals(repartidor.getId())) {
                log.warn("⚠️ Repartidor {} intentó modificar pedido {} que no le pertenece", 
                        repartidor.getNombre(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("No tienes permiso para modificar este pedido");
            }
            
            pedido.setEstado(request.estado());
            Pedido pedidoActualizado = pedidoRepository.save(pedido);
            
            log.info("✅ Repartidor {} actualizó pedido #{} a estado {}", 
                    repartidor.getNombre(), id, request.estado());
            return ResponseEntity.ok(pedidoActualizado);
            
        } catch (Exception e) {
            log.error("❌ Error al actualizar estado del pedido: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al actualizar estado del pedido");
        }
    }
    
    /**
     * 📍 Actualizar MI ubicación
     */
    @PutMapping("/actualizar-ubicacion")
    public ResponseEntity<?> actualizarUbicacion(@RequestBody UbicacionRequest request) {
        try {
            Usuario repartidor = obtenerRepartidorActual();
            
            repartidor.setLatitud(request.latitud());
            repartidor.setLongitud(request.longitud());
            usuarioRepository.save(repartidor);
            
            log.info("📍 Repartidor {} actualizó su ubicación: {}, {}", 
                    repartidor.getNombre(), request.latitud(), request.longitud());
            return ResponseEntity.ok("Ubicación actualizada correctamente");
            
        } catch (Exception e) {
            log.error("❌ Error al actualizar ubicación: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al actualizar ubicación");
        }
    }
    
    /**
     * 🟢🔴 Cambiar MI disponibilidad
     */
    @PutMapping("/disponibilidad")
    public ResponseEntity<?> cambiarDisponibilidad(@RequestBody DisponibilidadRequest request) {
        try {
            Usuario repartidor = obtenerRepartidorActual();
            
            repartidor.setDisponible(request.disponible());
            usuarioRepository.save(repartidor);
            
            log.info("🔄 Repartidor {} cambió disponibilidad a: {}", 
                    repartidor.getNombre(), request.disponible() ? "DISPONIBLE" : "NO DISPONIBLE");
            return ResponseEntity.ok("Disponibilidad actualizada correctamente");
            
        } catch (Exception e) {
            log.error("❌ Error al cambiar disponibilidad: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al cambiar disponibilidad");
        }
    }
    
    /**
     * 📊 Estadísticas de MI trabajo
     */
    @GetMapping("/mis-estadisticas")
    public ResponseEntity<?> obtenerEstadisticas() {
        try {
            Usuario repartidor = obtenerRepartidorActual();
            List<Pedido> pedidos = pedidoRepository.findByRepartidorId(repartidor.getId());
            
            long pendientes = pedidos.stream()
                    .filter(p -> p.getEstado() == Pedido.EstadoPedido.PENDIENTE)
                    .count();
            long enCamino = pedidos.stream()
                    .filter(p -> p.getEstado() == Pedido.EstadoPedido.EN_CAMINO)
                    .count();
            long entregados = pedidos.stream()
                    .filter(p -> p.getEstado() == Pedido.EstadoPedido.ENTREGADO)
                    .count();
            
            var estadisticas = new EstadisticasRepartidor(
                    pedidos.size(),
                    (int) pendientes,
                    (int) enCamino,
                    (int) entregados,
                    repartidor.getDisponible()
            );
            
            return ResponseEntity.ok(estadisticas);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener estadísticas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener estadísticas");
        }
    }
    
    // === MÉTODOS AUXILIARES ===
    
    private Usuario obtenerRepartidorActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Repartidor no encontrado"));
    }
    
    // DTOs
    private record EstadoRequest(Pedido.EstadoPedido estado) {}
    private record UbicacionRequest(Double latitud, Double longitud) {}
    private record DisponibilidadRequest(Boolean disponible) {}
    
    private record EstadisticasRepartidor(
            int totalPedidos,
            int pendientes,
            int enCamino,
            int entregados,
            boolean disponible
    ) {}
}