package com.Zygo.proyecto.service;

import com.Zygo.proyecto.dto.PedidoDTO;
import com.Zygo.proyecto.model.Pedido;
import com.Zygo.proyecto.model.Pedido.EstadoPedido;
import com.Zygo.proyecto.model.Usuario;
import com.Zygo.proyecto.repository.PedidoRepository;
import com.Zygo.proyecto.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class PedidoService {
    
    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);
    
    @Autowired
    private PedidoRepository pedidoRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Transactional
    public PedidoDTO crearPedido(PedidoDTO dto) {
        log.info("Creando nuevo pedido para cliente ID: {}", dto.getClienteId());
        
        Usuario cliente = usuarioRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setDescripcion(dto.getDescripcion());
        pedido.setDireccionOrigen(dto.getDireccionOrigen());
        pedido.setDireccionDestino(dto.getDireccionDestino());
        pedido.setDistanciaKm(dto.getDistanciaKm());
        pedido.setCosto(calcularCosto(dto.getDistanciaKm()));
        pedido.setEstado(EstadoPedido.PENDIENTE);
        
        Pedido guardado = pedidoRepository.save(pedido);
        log.info("Pedido creado con ID: {}", guardado.getId());
        
        // Asignar repartidor de forma asíncrona
        asignarRepartidorAsync(guardado.getId());
        
        return convertirEntidadADto(guardado);
    }
    
    @Async("taskExecutor")
    public CompletableFuture<Void> asignarRepartidorAsync(Long pedidoId) {
        log.info("[HILO ASYNC] Iniciando asignación automática para pedido ID: {}", pedidoId);
        
        try {
            Thread.sleep(2000); // Simula búsqueda de repartidor
            
            Pedido pedido = pedidoRepository.findById(pedidoId)
                    .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
            
            if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
                log.warn("[HILO ASYNC] Pedido {} ya no está pendiente", pedidoId);
                return CompletableFuture.completedFuture(null);
            }
            
            // Buscar repartidor disponible
            List<Usuario> repartidores = usuarioRepository
                    .findRepartidoresActivos(Usuario.TipoUsuario.REPARTIDOR);
            
            if (!repartidores.isEmpty()) {
                Usuario repartidor = repartidores.get(0); // Asigna el primero disponible
                asignarRepartidor(pedidoId, repartidor.getId());
                log.info("[HILO ASYNC] Repartidor {} asignado al pedido {}", 
                        repartidor.getId(), pedidoId);
            } else {
                log.warn("[HILO ASYNC] No hay repartidores disponibles para pedido {}", pedidoId);
            }
            
        } catch (InterruptedException e) {
            log.error("[HILO ASYNC] Error en asignación automática", e);
            Thread.currentThread().interrupt();
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    @Transactional
    public PedidoDTO asignarRepartidor(Long pedidoId, Long repartidorId) {
        log.info("Asignando repartidor {} al pedido {}", repartidorId, pedidoId);
        
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        
        Usuario repartidor = usuarioRepository.findById(repartidorId)
                .orElseThrow(() -> new RuntimeException("Repartidor no encontrado"));
        
        pedido.setRepartidor(repartidor);
        pedido.setEstado(EstadoPedido.ASIGNADO);
        pedido.setFechaAsignacion(LocalDateTime.now());
        
        Pedido actualizado = pedidoRepository.save(pedido);
        log.info("Pedido asignado exitosamente");
        
        // Simular inicio de entrega de forma asíncrona
        simularEntregaAsync(pedidoId);
        
        return convertirEntidadADto(actualizado);
    }
    
    @Async("taskExecutor")
    public CompletableFuture<Void> simularEntregaAsync(Long pedidoId) {
        log.info("[HILO ASYNC] Simulando entrega para pedido {}", pedidoId);
        
        try {
            Thread.sleep(5000); // Simula tiempo de entrega
            
            Pedido pedido = pedidoRepository.findById(pedidoId).orElse(null);
            if (pedido != null && pedido.getEstado() == EstadoPedido.ASIGNADO) {
                actualizarEstadoPedido(pedidoId, EstadoPedido.EN_CAMINO);
                log.info("[HILO ASYNC] Pedido {} ahora está en camino", pedidoId);
                
                Thread.sleep(5000); // Simula llegada
                actualizarEstadoPedido(pedidoId, EstadoPedido.ENTREGADO);
                log.info("[HILO ASYNC] Pedido {} entregado exitosamente", pedidoId);
            }
            
        } catch (InterruptedException e) {
            log.error("[HILO ASYNC] Error simulando entrega", e);
            Thread.currentThread().interrupt();
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    @Transactional
    public PedidoDTO actualizarEstadoPedido(Long pedidoId, EstadoPedido nuevoEstado) {
        log.info("Actualizando estado del pedido {} a {}", pedidoId, nuevoEstado);
        
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        
        pedido.setEstado(nuevoEstado);
        
        if (nuevoEstado == EstadoPedido.ENTREGADO) {
            pedido.setFechaEntrega(LocalDateTime.now());
        }
        
        Pedido actualizado = pedidoRepository.save(pedido);
        return convertirEntidadADto(actualizado);
    }
    
    @Transactional(readOnly = true)
    public PedidoDTO obtenerPedidoPorId(Long id) {
        log.debug("Buscando pedido con ID: {}", id);
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        return convertirEntidadADto(pedido);
    }
    
    @Transactional(readOnly = true)
    public List<PedidoDTO> obtenerTodosLosPedidos() {
        log.debug("Obteniendo todos los pedidos");
        return pedidoRepository.findAll().stream()
                .map(this::convertirEntidadADto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<PedidoDTO> obtenerPedidosPorCliente(Long clienteId) {
        log.debug("Obteniendo pedidos del cliente: {}", clienteId);
        return pedidoRepository.findByClienteId(clienteId).stream()
                .map(this::convertirEntidadADto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<PedidoDTO> obtenerPedidosPorRepartidor(Long repartidorId) {
        log.debug("Obteniendo pedidos del repartidor: {}", repartidorId);
        return pedidoRepository.findByRepartidorId(repartidorId).stream()
                .map(this::convertirEntidadADto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<PedidoDTO> obtenerPedidosPendientes() {
        log.debug("Obteniendo pedidos pendientes");
        return pedidoRepository.findByEstado(EstadoPedido.PENDIENTE).stream()
                .map(this::convertirEntidadADto)
                .collect(Collectors.toList());
    }
    
    private Double calcularCosto(Double distanciaKm) {
        // Tarifa base: $5000 + $2000 por km
        return 5000.0 + (distanciaKm * 2000.0);
    }
    
    private PedidoDTO convertirEntidadADto(Pedido pedido) {
        PedidoDTO dto = new PedidoDTO();
        dto.setId(pedido.getId());
        dto.setClienteId(pedido.getCliente().getId());
        dto.setNombreCliente(pedido.getCliente().getNombre());
        
        if (pedido.getRepartidor() != null) {
            dto.setRepartidorId(pedido.getRepartidor().getId());
            dto.setNombreRepartidor(pedido.getRepartidor().getNombre());
        }
        
        dto.setDescripcion(pedido.getDescripcion());
        dto.setDireccionOrigen(pedido.getDireccionOrigen());
        dto.setDireccionDestino(pedido.getDireccionDestino());
        dto.setDistanciaKm(pedido.getDistanciaKm());
        dto.setCosto(pedido.getCosto());
        dto.setEstado(pedido.getEstado());
        dto.setFechaCreacion(pedido.getFechaCreacion());
        dto.setFechaAsignacion(pedido.getFechaAsignacion());
        dto.setFechaEntrega(pedido.getFechaEntrega());
        
        return dto;
    }
}