package com.Zygo.proyecto.service;

import com.Zygo.proyecto.dto.RutaOptimaDTO;
import com.Zygo.proyecto.model.*;
import com.Zygo.proyecto.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 🎯 SERVICIO DE ASIGNACIÓN AUTOMÁTICA
 * Integra Dijkstra + Ubicaciones + Asignación de repartidores
 * 
 * Flujo:
 * 1. Cliente crea pedido con su ubicación GPS
 * 2. Sistema busca restaurante más cercano
 * 3. Sistema busca repartidor disponible más cercano
 * 4. Calcula ruta óptima: Repartidor → Restaurante → Cliente
 * 5. Asigna automáticamente y comienza entrega
 */
@Service
public class AsignacionService {
    
    private static final Logger log = LoggerFactory.getLogger(AsignacionService.class);
    
    // Distancia máxima para considerar un repartidor disponible (en km)
    private static final double DISTANCIA_MAXIMA_REPARTIDOR = 10.0;
    
    @Autowired
    private PedidoRepository pedidoRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private GraphRepository graphRepository;
    
    @Autowired
    private DijkstraServiceOptimizado dijkstraService;
    
    @Autowired
    private LugarService lugarService;
    
    @Autowired
    private GraphManagementService graphManagementService;
    
    /**
     * 🎯 MÉTODO PRINCIPAL: Asignación automática completa
     * Se llama cuando un cliente crea un pedido
     */
    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<Void> asignarPedidoAutomatico(Long pedidoId) {
        log.info("🔄 INICIANDO ASIGNACIÓN AUTOMÁTICA para pedido {}", pedidoId);
        
        try {
            Pedido pedido = pedidoRepository.findById(pedidoId)
                    .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
            
            // Validar que el pedido esté en estado PENDIENTE
            if (pedido.getEstado() != Pedido.EstadoPedido.PENDIENTE) {
                log.warn("⚠️ Pedido {} no está PENDIENTE, estado actual: {}", 
                        pedidoId, pedido.getEstado());
                return CompletableFuture.completedFuture(null);
            }
            
            long tiempoInicio = System.currentTimeMillis();
            
            // PASO 1: Encontrar restaurante más cercano
            log.info("🍽️ PASO 1: Buscando restaurante más cercano...");
            Graph restaurante = encontrarRestauranteMasCercano(
                pedido.getLatOrigen(), 
                pedido.getLonOrigen()
            );
            
            if (restaurante == null) {
                log.error("❌ No hay restaurantes disponibles");
                pedido.setEstado(Pedido.EstadoPedido.CANCELADO);
                pedidoRepository.save(pedido);
                return CompletableFuture.completedFuture(null);
            }
            
            pedido.setRestaurante(restaurante);
            log.info("✅ Restaurante asignado: {} (ID: {})", restaurante.getNombre(), restaurante.getId());
            
            // PASO 2: Encontrar repartidor más cercano disponible
            log.info("🚴 PASO 2: Buscando repartidor más cercano disponible...");
            Usuario repartidor = encontrarRepartidorMasCercano(
                restaurante.getLatitud(), 
                restaurante.getLongitud()
            );
            
            if (repartidor == null) {
                log.error("❌ No hay repartidores disponibles");
                pedido.setEstado(Pedido.EstadoPedido.CANCELADO);
                pedidoRepository.save(pedido);
                return CompletableFuture.completedFuture(null);
            }
            
            pedido.setRepartidor(repartidor);
            log.info("✅ Repartidor asignado: {} (ID: {})", repartidor.getNombre(), repartidor.getId());
            
            // PASO 3: Calcular ruta óptima completa
            log.info("📍 PASO 3: Calculando ruta óptima...");
            RutaOptimaDTO ruta = calcularRutaCompleta(pedido, restaurante);
            
            if (ruta == null) {
                log.error("❌ No se pudo calcular la ruta");
                pedido.setEstado(Pedido.EstadoPedido.CANCELADO);
                pedidoRepository.save(pedido);
                return CompletableFuture.completedFuture(null);
            }
            
            // PASO 4: Actualizar pedido con información calculada
            log.info("🔄 PASO 4: Actualizando información del pedido...");
            actualizarPedidoConRuta(pedido, ruta, restaurante);
            pedido.setEstado(Pedido.EstadoPedido.ASIGNADO);
            pedido.setFechaAsignacion(LocalDateTime.now());
            
            Pedido pedidoActualizado = pedidoRepository.save(pedido);
            
            // PASO 5: Marcar repartidor como no disponible
            repartidor.setDisponible(false);
            usuarioRepository.save(repartidor);
            
            long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
            log.info("✅ ASIGNACIÓN COMPLETADA EN {}ms", tiempoTotal);
            log.info("📊 RESUMEN:");
            log.info("   - Restaurante: {}", restaurante.getNombre());
            log.info("   - Repartidor: {}", repartidor.getNombre());
            log.info("   - Distancia total: {:.2f} km", ruta.getDistanciaTotalKm());
            log.info("   - Tiempo estimado: {} minutos", ruta.getTiempoEstimadoMinutos());
            log.info("   - Costo estimado: ${:.0f}", ruta.getCostoEstimado());
            
        } catch (Exception e) {
            log.error("❌ ERROR EN ASIGNACIÓN AUTOMÁTICA: {}", e.getMessage(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 🍽️ Encuentra el restaurante más cercano a una ubicación
     */
    private Graph encontrarRestauranteMasCercano(Double lat, Double lon) {
        log.debug("🔍 Buscando restaurante cercano a ({}, {})", lat, lon);
        
        // Obtener todos los restaurantes activos
        List<Graph> restaurantes = graphRepository.findByTipo(Graph.TipoNodo.RESTAURANTE);
        
        if (restaurantes.isEmpty()) {
            log.warn("⚠️ No hay restaurantes disponibles en la BD");
            return null;
        }
        
        // Encontrar el más cercano por distancia euclidiana
        Graph restauranteMasCercano = null;
        double distanciaMinima = Double.MAX_VALUE;
        
        for (Graph rest : restaurantes) {
            double distancia = calcularDistancia(lat, lon, rest.getLatitud(), rest.getLongitud());
            
            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
                restauranteMasCercano = rest;
            }
        }
        
        if (restauranteMasCercano != null) {
            log.info("✅ Restaurante encontrado: {} ({:.2f} km)", 
                    restauranteMasCercano.getNombre(), distanciaMinima);
        }
        
        return restauranteMasCercano;
    }
    
    /**
     * 🚴 Encuentra el repartidor disponible más cercano
     */
    private Usuario encontrarRepartidorMasCercano(Double lat, Double lon) {
        log.debug("🔍 Buscando repartidor disponible cerca de ({}, {})", lat, lon);
        
        // Obtener repartidores disponibles
        List<Usuario> repartidoresDisponibles = usuarioRepository.findAll().stream()
                .filter(u -> u.getTipo() == Usuario.TipoUsuario.REPARTIDOR)
                .filter(u -> u.getActivo())
                .filter(u -> u.getDisponible() != null && u.getDisponible())
                .collect(Collectors.toList());
        
        if (repartidoresDisponibles.isEmpty()) {
            log.warn("⚠️ No hay repartidores disponibles");
            return null;
        }
        
        log.info("📊 Repartidores disponibles: {}", repartidoresDisponibles.size());
        
        // Encontrar el más cercano
        Usuario repartidorMasCercano = null;
        double distanciaMinima = Double.MAX_VALUE;
        
        for (Usuario rep : repartidoresDisponibles) {
            if (rep.getLatitud() == null || rep.getLongitud() == null) {
                // Si no tiene coordenadas, usar ubicación por defecto
                rep.setLatitud(5.73);
                rep.setLongitud(-72.93);
            }
            
            double distancia = calcularDistancia(lat, lon, rep.getLatitud(), rep.getLongitud());
            
            // Solo considerar si está dentro del radio máximo
            if (distancia <= DISTANCIA_MAXIMA_REPARTIDOR && distancia < distanciaMinima) {
                distanciaMinima = distancia;
                repartidorMasCercano = rep;
            }
        }
        
        if (repartidorMasCercano != null) {
            log.info("✅ Repartidor encontrado: {} ({:.2f} km)", 
                    repartidorMasCercano.getNombre(), distanciaMinima);
        } else {
            log.warn("⚠️ No hay repartidores dentro del radio de {} km", DISTANCIA_MAXIMA_REPARTIDOR);
        }
        
        return repartidorMasCercano;
    }
    
    /**
     * 📍 Calcula la ruta completa: Repartidor → Restaurante → Cliente
     */
    private RutaOptimaDTO calcularRutaCompleta(Pedido pedido, Graph restaurante) {
        log.debug("🗺️ Calculando ruta completa para pedido {}", pedido.getId());
        
        try {
            // Encontrar nodo más cercano del cliente
            Graph nodoCliente = lugarService.encontrarNodoMasCercano(
                pedido.getLatDestino(), 
                pedido.getLonDestino()
            );
            
            if (nodoCliente == null) {
                log.error("❌ No se encontró nodo cercano al cliente");
                return null;
            }
            
            pedido.setNodoCliente(nodoCliente);
            pedido.setNodoRepartidor(restaurante); // Temporalmente
            
            // Calcular ruta completa usando Dijkstra
            RutaOptimaDTO ruta = dijkstraService.encontrarRutaOptima(
                restaurante.getId(),
                nodoCliente.getId(),
                true // Considerar tráfico
            );
            
            if (ruta != null) {
                log.info("✅ Ruta calculada: {:.2f} km en {} minutos", 
                        ruta.getDistanciaTotalKm(), 
                        ruta.getTiempoEstimadoMinutos());
            }
            
            return ruta;
            
        } catch (Exception e) {
            log.error("❌ Error calculando ruta: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 🔄 Actualiza el pedido con la información de la ruta
     */
    private void actualizarPedidoConRuta(Pedido pedido, RutaOptimaDTO ruta, Graph restaurante) {
        // Actualizar distancia y costo con los valores calculados
        if (ruta.getDistanciaTotalKm() != null) {
            pedido.setDistanciaKm(ruta.getDistanciaTotalKm());
            
            // Recalcular costo: $5000 base + $2000 por km
            Double nuevoCosto = 5000.0 + (ruta.getDistanciaTotalKm() * 2000.0);
            pedido.setCosto(nuevoCosto);
        }
        
        if (ruta.getTiempoEstimadoMinutos() != null) {
            log.info("⏱️ Tiempo estimado actualizado: {} minutos", ruta.getTiempoEstimadoMinutos());
        }
    }
    
    /**
     * 📊 Obtiene estadísticas de repartidores
     */
    public Map<String, Object> obtenerEstadisticasRepartidores() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Usuario> repartidores = usuarioRepository.findAll().stream()
                .filter(u -> u.getTipo() == Usuario.TipoUsuario.REPARTIDOR)
                .collect(Collectors.toList());
        
        long disponibles = repartidores.stream()
                .filter(u -> u.getDisponible() != null && u.getDisponible())
                .count();
        
        long ocupados = repartidores.stream()
                .filter(u -> u.getDisponible() == null || !u.getDisponible())
                .count();
        
        stats.put("total", repartidores.size());
        stats.put("disponibles", disponibles);
        stats.put("ocupados", ocupados);
        stats.put("porcentajeDisponibilidad", 
                repartidores.isEmpty() ? 0 : (disponibles * 100.0 / repartidores.size()));
        
        return stats;
    }
    
    /**
     * 📍 Obtiene estadísticas de restaurantes
     */
    public Map<String, Object> obtenerEstadisticasRestaurantes() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Graph> restaurantes = graphRepository.findByTipo(Graph.TipoNodo.RESTAURANTE);
        
        stats.put("total", restaurantes.size());
        stats.put("restaurantes", restaurantes.stream()
                .map(r -> Map.of(
                    "id", r.getId(),
                    "nombre", r.getNombre(),
                    "coordenadas", Map.of("lat", r.getLatitud(), "lon", r.getLongitud())
                ))
                .collect(Collectors.toList()));
        
        return stats;
    }
    
    /**
     * 🧮 Calcula distancia entre dos coordenadas (Haversine simplificado)
     */
    private double calcularDistancia(Double lat1, Double lon1, Double lat2, Double lon2) {
        final double R = 6371; // Radio de la Tierra en km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}