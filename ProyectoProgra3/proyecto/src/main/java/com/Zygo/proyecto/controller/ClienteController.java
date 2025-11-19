// ========================================
// 📍 ACTUALIZAR: ClienteController.java
// ========================================

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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * 📍 NUEVO: Guardar ubicación del cliente en tiempo real
     * POST /api/cliente/guardar-ubicacion
     * {
     *   "latitud": 5.7147,
     *   "longitud": -72.9341
     * }
     */
    @PostMapping("/guardar-ubicacion")
    public ResponseEntity<?> guardarUbicacion(@RequestBody UbicacionRequest request) {
        try {
            Usuario cliente = obtenerClienteActual();
            
            // Validar coordenadas
            if (request.latitud() == null || request.longitud() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Latitud y longitud son requeridas"));
            }
            
            if (request.latitud() < -90 || request.latitud() > 90) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Latitud debe estar entre -90 y 90"));
            }
            
            if (request.longitud() < -180 || request.longitud() > 180) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Longitud debe estar entre -180 y 180"));
            }
            
            // Actualizar ubicación del cliente
            cliente.setLatitud(request.latitud());
            cliente.setLongitud(request.longitud());
            usuarioRepository.save(cliente);
            
            log.info("📍 Cliente {} guardó ubicación: ({}, {})", 
                    cliente.getNombre(), request.latitud(), request.longitud());
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Ubicación guardada exitosamente");
            response.put("latitud", cliente.getLatitud());
            response.put("longitud", cliente.getLongitud());
            response.put("nombreCliente", cliente.getNombre());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error al guardar ubicación: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar ubicación"));
        }
    }
    
    /**
     * 📍 NUEVO: Obtener mi ubicación actual
     * GET /api/cliente/mi-ubicacion
     */
    @GetMapping("/mi-ubicacion")
    public ResponseEntity<?> obtenerMiUbicacion() {
        try {
            Usuario cliente = obtenerClienteActual();
            
            if (cliente.getLatitud() == null || cliente.getLongitud() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Ubicación no registrada"));
            }
            
            Map<String, Object> ubicacion = new HashMap<>();
            ubicacion.put("latitud", cliente.getLatitud());
            ubicacion.put("longitud", cliente.getLongitud());
            ubicacion.put("nombre", cliente.getNombre());
            ubicacion.put("email", cliente.getEmail());
            
            return ResponseEntity.ok(ubicacion);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener ubicación: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener ubicación"));
        }
    }
    
    /**
     * 🔍 NUEVO: Obtener todos los CLIENTES (solo para admin)
     * Esto es para que el admin vea a todos los clientes en el mapa
     * GET /api/cliente/todos
     */
    @GetMapping("/todos")
    @PreAuthorize("hasRole('ADMIN')")  // 🔐 Solo admin
    public ResponseEntity<?> obtenerTodosLosClientes() {
        try {
            List<Usuario> clientes = usuarioRepository.findByTipo(Usuario.TipoUsuario.CLIENTE);
            
            log.info("👥 Admin solicitó lista de {} clientes", clientes.size());
            
            return ResponseEntity.ok(clientes);
            
        } catch (Exception e) {
            log.error("❌ Error al obtener clientes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener clientes"));
        }
    }
    
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
     * 📦 Ver UN pedido MÍO especifico
     */
    @GetMapping("/mis-pedidos/{id}")
    public ResponseEntity<?> obtenerMiPedidoPorId(@PathVariable Long id) {
        try {
            Usuario cliente = obtenerClienteActual();
            
            Pedido pedido = pedidoRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
            
            // 🔐 Verificar que el pedido es del cliente
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
     * ➕ Crear un NUEVO pedido con ubicación
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
            
            // Guardar coordenadas
            if (request.latOrigen() != null) {
                pedido.setLatOrigen(request.latOrigen());
                pedido.setLonOrigen(request.lonOrigen());
                pedido.setLatDestino(request.latDestino());
                pedido.setLonDestino(request.lonDestino());
            }
            
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
            
            // 🔐 Verificar que el pedido es del cliente
            if (!pedido.getCliente().getId().equals(cliente.getId())) {
                log.warn("⚠️ Cliente {} intentó cancelar pedido {} que no le pertenece", 
                        cliente.getNombre(), id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("No tienes permiso para cancelar este pedido");
            }
            
            // 🔐 Solo se puede cancelar si está PENDIENTE
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
    private record UbicacionRequest(
            Double latitud,
            Double longitud
    ) {}
    
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



    /**
 * ✅ NUEVO: Guardar ubicación del cliente
 */
@PutMapping("/guardar-ubicacion")
public ResponseEntity<?> guardarUbicacion(
        @RequestParam Double latitud,
        @RequestParam Double longitud,
        @AuthenticationPrincipal UserDetails userDetails) {
    
    try {
        String email = userDetails.getUsername();
        Usuario cliente = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        if (cliente.getTipo() != Usuario.TipoUsuario.CLIENTE) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Solo los clientes pueden guardar ubicación"
            ));
        }
        
        // Guardar ubicación
        cliente.setLatitud(latitud);
        cliente.setLongitud(longitud);
        usuarioRepository.save(cliente);
        
        log.info("✅ Ubicación guardada para cliente {}: ({}, {})", 
                cliente.getNombre(), latitud, longitud);
        
        return ResponseEntity.ok(Map.of(
            "mensaje", "Ubicación guardada exitosamente",
            "latitud", latitud,
            "longitud", longitud
        ));
        
    } catch (Exception e) {
        log.error("❌ Error guardando ubicación: {}", e.getMessage());
        return ResponseEntity.internalServerError().body(Map.of(
            "error", "Error al guardar ubicación: " + e.getMessage()
        ));
    }
}

/**
 * ✅ NUEVO: Obtener perfil del cliente con ubicación
 */
@GetMapping("/mi-perfil")
public ResponseEntity<?> obtenerMiPerfil(@AuthenticationPrincipal UserDetails userDetails) {
    try {
        String email = userDetails.getUsername();
        Usuario cliente = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        Map<String, Object> perfil = new HashMap<>();
        perfil.put("id", cliente.getId());
        perfil.put("nombre", cliente.getNombre());
        perfil.put("email", cliente.getEmail());
        perfil.put("telefono", cliente.getTelefono());
        perfil.put("direccion", cliente.getDireccion());
        perfil.put("latitud", cliente.getLatitud());
        perfil.put("longitud", cliente.getLongitud());
        perfil.put("tipo", cliente.getTipo());
        
        return ResponseEntity.ok(perfil);
        
    } catch (Exception e) {
        log.error("❌ Error obteniendo perfil: {}", e.getMessage());
        return ResponseEntity.internalServerError().body(Map.of(
            "error", "Error al obtener perfil"
        ));
    }
}
}