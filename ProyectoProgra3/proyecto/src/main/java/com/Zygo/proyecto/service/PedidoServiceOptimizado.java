package com.Zygo.proyecto.service;

import com.Zygo.proyecto.dto.PedidoDTO;
import com.Zygo.proyecto.dto.RutaOptimaDTO;
import com.Zygo.proyecto.model.Pedido;
import com.Zygo.proyecto.model.Pedido.EstadoPedido;
import com.Zygo.proyecto.model.Usuario;
import com.Zygo.proyecto.model.Graph;
import com.Zygo.proyecto.repository.PedidoRepository;
import com.Zygo.proyecto.repository.UsuarioRepository;
import com.Zygo.proyecto.repository.GraphRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Servicio mejorado para gestión de pedidos con optimización de rutas usando Dijkstra
 */
@Service
public class PedidoServiceOptimizado {
    
    private static final Logger log = LoggerFactory.getLogger(PedidoServiceOptimizado.class);
    
    @Autowired
    private PedidoRepository pedidoRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private GraphRepository graphRepository;
    
    @Autowired
    private DijkstraService dijkstraService;
    
    @Autowired
    private GraphManagementService graphManagementService;
    
    @Transactional
    public PedidoDTO crearPedidoConRutaOptima(PedidoDTO dto) {
        log.info("Creando nuevo pedido con ruta óptima para cliente ID: {}", dto.getClienteId());
        
        Usuario cliente = usuarioRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        
        // Crear pedido básico
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setDescripcion(dto.getDescripcion());
        pedido.setDireccionOrigen(dto.getDireccionOrigen());
        pedido.setDireccionDestino(dto.getDireccionDestino());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        
        // Calcular ruta óptima y distancia real usando Dijkstra
        try {
            RutaOptimaDTO rutaOptima = calcularRutaOptimaPedido(
                    dto.getDireccionOrigen(), 
                    dto.getDireccionDestino()
            );
            
            // Usar la distancia calculada por Dijkstra
            pedido.setDistanciaKm(rutaOptima.getDistanciaTotalKm());
            pedido.setCosto(rutaOptima.getCostoEstimado());
            
            log.info("Ruta óptima calculada: {} km, tiempo estimado: {} min", 
                     rutaOptima.getDistanciaTotalKm(), 
                     rutaOptima.getTiempoEstimadoMinutos());
        } catch (Exception e) {
            log.warn("No se pudo calcular ruta óptima, usando distancia proporcionada: {}", e.getMessage());
            pedido.setDistanciaKm(dto.getDistanciaKm());
            pedido.setCosto(calcularCosto(dto.getDistanciaKm()));
        }
        
        Pedido guardado = pedidoRepository.save(pedido);
        log.info("Pedido creado con ID: {}", guardado.getId());
        
        // Asignar repartidor óptimo de forma asíncrona
        asignarRepartidorOptimoAsync(guardado.getId());
        
        return convertirEntidadADto(guardado);
    }
    
    /**
     * Calcula la ruta óptima para un pedido
     */
    private RutaOptimaDTO calcularRutaOptimaPedido(String direccionOrigen, String direccionDestino) {
        // Encontrar nodos más cercanos a las direcciones
        Graph nodoOrigen = encontrarNodoMasCercano(direccionOrigen, Graph.TipoNodo.RESTAURANTE);
        Graph nodoDestino = encontrarNodoMasCercano(direccionDestino, Graph.TipoNodo.CLIENTE);
        
        if (nodoOrigen == null || nodoDestino == null) {
            throw new RuntimeException("No se pudieron encontrar nodos para las direcciones");
        }
        
        // Calcular ruta óptima considerando tráfico
        return dijkstraService.encontrarRutaOptima(
                nodoOrigen.getId(), 
                nodoDestino.getId(), 
                true // considerar tráfico
        );
    }
    
    /**
     * Encuentra el nodo más cercano a una dirección
     */
    private Graph encontrarNodoMasCercano(String direccion, Graph.TipoNodo tipoPreferido) {
        List<Graph> nodosCandidatos = graphRepository.findByTipo(tipoPreferido);
        
        if (nodosCandidatos.isEmpty()) {
            nodosCandidatos = graphRepository.findAll();
        }
        
        if (!nodosCandidatos.isEmpty()) {
            // Por ahora retornamos un nodo basado en hash de la dirección
            // En producción, usar geocoding real
            return nodosCandidatos.get(Math.abs(direccion.hashCode()) % nodosCandidatos.size());
        }
        
        return null;
    }
    
    @Async("taskExecutor")
    public CompletableFuture<Void> asignarRepartidorOptimoAsync(Long pedidoId) {
        log.info("[ASYNC] Buscando repartidor óptimo para pedido ID: {}", pedidoId);
        
        try {
            Thread.sleep(2000); // Simula procesamiento
            
            Pedido pedido = pedidoRepository.findById(pedidoId)
                    .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
            
            if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
                log.warn("[ASYNC] Pedido {} ya no está pendiente", pedidoId);
                return CompletableFuture.completedFuture(null);
            }
            
            // Buscar el repartidor más cercano usando Dijkstra
            Usuario repartidorOptimo = encontrarRepartidorMasCercano(pedido);
            
            if (repartidorOptimo != null) {
                asignarRepartidorConRuta(pedidoId, repartidorOptimo.getId());
                log.info("[ASYNC] Repartidor óptimo {} asignado al pedido {}", 
                        repartidorOptimo.getId(), pedidoId);
            } else {
                log.warn("[ASYNC] No hay repartidores disponibles para pedido {}", pedidoId);
            }
            
        } catch (InterruptedException e) {
            log.error("[ASYNC] Error en asignación automática", e);
            Thread.currentThread().interrupt();
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Encuentra el repartidor más cercano al pedido usando Dijkstra
     */
    private Usuario encontrarRepartidorMasCercano(Pedido pedido) {
        List<Usuario> repartidoresDisponibles = usuarioRepository
                .findRepartidoresActivos(Usuario.TipoUsuario.REPARTIDOR);
        
        if (repartidoresDisponibles.isEmpty()) {
            return null;
        }
        
        // Por simplicidad, verificar carga de trabajo de cada repartidor
        Usuario mejorRepartidor = null;
        Long menorCantidadPedidos = Long.MAX_VALUE;
        
        for (Usuario repartidor : repartidoresDisponibles) {
            Long pedidosActivos = pedidoRepository
                    .countPedidosActivosPorRepartidor(repartidor.getId());
            
            if (pedidosActivos < menorCantidadPedidos) {
                menorCantidadPedidos = pedidosActivos;
                mejorRepartidor = repartidor;
            }
        }
        
        return mejorRepartidor;
    }
    
    @Transactional
    public PedidoDTO asignarRepartidorConRuta(Long pedidoId, Long repartidorId) {
        log.info("Asignando repartidor {} al pedido {} con ruta óptima", repartidorId, pedidoId);
        
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        
        Usuario repartidor = usuarioRepository.findById(repartidorId)
                .orElseThrow(() -> new RuntimeException("Repartidor no encontrado"));
        
        pedido.setRepartidor(repartidor);
        pedido.setEstado(EstadoPedido.ASIGNADO);
        pedido.setFechaAsignacion(LocalDateTime.now());
        
        // Calcular y almacenar la ruta óptima
        try {
            RutaOptimaDTO rutaOptima = graphManagementService.calcularRutaPedido(pedidoId);
            log.info("Ruta óptima calculada para entrega: {} km en {} minutos", 
                     rutaOptima.getDistanciaTotalKm(), 
                     rutaOptima.getTiempoEstimadoMinutos());
        } catch (Exception e) {
            log.error("Error calculando ruta óptima: {}", e.getMessage());
        }
        
        Pedido actualizado = pedidoRepository.save(pedido);
        log.info("Pedido asignado exitosamente con ruta óptima");
        
        // Iniciar simulación de entrega
        simularEntregaConRutaAsync(pedidoId);
        
        return convertirEntidadADto(actualizado);
    }
    
    @Async("taskExecutor")
    public CompletableFuture<Void> simularEntregaConRutaAsync(Long pedidoId) {
        log.info("[ASYNC] Simulando entrega con ruta óptima para pedido {}", pedidoId);
        
        try {
            // Obtener información de la ruta
            RutaOptimaDTO ruta = graphManagementService.calcularRutaPedido(pedidoId);
            
            // Simular tiempo de recogida
            Thread.sleep(3000);
            actualizarEstadoPedido(pedidoId, EstadoPedido.EN_CAMINO);
            log.info("[ASYNC] Pedido {} en camino", pedidoId);
            
            // Simular tiempo de entrega basado en la ruta real
            int tiempoEntregaMs = ruta.getTiempoEstimadoMinutos() * 1000; // Convertir a milisegundos para simulación
            Thread.sleep(Math.min(tiempoEntregaMs, 10000)); // Máximo 10 segundos para la simulación
            
            actualizarEstadoPedido(pedidoId, EstadoPedido.ENTREGADO);
            log.info("[ASYNC] Pedido {} entregado exitosamente después de recorrer {} km", 
                     pedidoId, ruta.getDistanciaTotalKm());
            
        } catch (Exception e) {
            log.error("[ASYNC] Error simulando entrega", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Obtiene las rutas óptimas para múltiples pedidos de un repartidor
     */
    @Transactional(readOnly = true)
    public List<RutaOptimaDTO> obtenerRutasOptimasRepartidor(Long repartidorId) {
        log.info("Calculando rutas óptimas para repartidor {}", repartidorId);
        
        // Obtener pedidos activos del repartidor
        List<Pedido> pedidosActivos = pedidoRepository.findByRepartidorId(repartidorId).stream()
                .filter(p -> p.getEstado() == EstadoPedido.ASIGNADO || 
                           p.getEstado() == EstadoPedido.EN_CAMINO)
                .collect(Collectors.toList());
        
        if (pedidosActivos.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Calcular rutas óptimas para todos los pedidos
        return graphManagementService.calcularRutasRepartidor(repartidorId);
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