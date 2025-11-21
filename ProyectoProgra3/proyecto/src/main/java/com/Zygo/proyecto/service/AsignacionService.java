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
 * 🎯 SERVICIO DE ASIGNACIÓN AUTOMÁTICA CON 2 RUTAS
 * 
 * FLUJO COMPLETO:
 * 1. Repartidor → Restaurante (pickup)
 * 2. Restaurante → Cliente (delivery)
 */
@Service
public class AsignacionService {
    
    private static final Logger log = LoggerFactory.getLogger(AsignacionService.class);
    private static final double DISTANCIA_MAXIMA_REPARTIDOR = 10.0;
    private static final int TIEMPO_PREPARACION_MIN = 10; // Tiempo que tarda el restaurante en preparar
    
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
    private HistorialRutaService historialRutaService;
    
    /**
     * 🎯 MÉTODO PRINCIPAL: Asignación automática con 2 rutas
     */
    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<Void> asignarPedidoAutomatico(Long pedidoId) {
        log.info("🔄 INICIANDO ASIGNACIÓN AUTOMÁTICA para pedido {}", pedidoId);
        
        try {
            Pedido pedido = pedidoRepository.findById(pedidoId)
                    .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
            
            if (pedido.getEstado() != Pedido.EstadoPedido.PENDIENTE) {
                log.warn("⚠️ Pedido {} no está PENDIENTE, estado actual: {}", 
                        pedidoId, pedido.getEstado());
                return CompletableFuture.completedFuture(null);
            }
            
            long tiempoInicio = System.currentTimeMillis();
            
            // ================== PASO 1: ENCONTRAR RESTAURANTE ==================
            log.info("🍽️ PASO 1: Buscando restaurante más cercano al cliente...");
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
            
            // ================== PASO 2: ENCONTRAR REPARTIDOR ==================
            log.info("🚴 PASO 2: Buscando repartidor más cercano al restaurante...");
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
            
            // ================== PASO 3: CALCULAR RUTA 1 (Repartidor → Restaurante) ==================
            log.info("🗺️ PASO 3A: Calculando RUTA 1 - Repartidor → Restaurante (PICKUP)...");
            
            // Encontrar nodo más cercano al repartidor
            Graph nodoRepartidor = lugarService.encontrarNodoMasCercano(
                repartidor.getLatitud(), 
                repartidor.getLongitud()
            );
            
            if (nodoRepartidor == null) {
                log.error("❌ No se encontró nodo cercano al repartidor");
                pedido.setEstado(Pedido.EstadoPedido.CANCELADO);
                pedidoRepository.save(pedido);
                return CompletableFuture.completedFuture(null);
            }
            
            pedido.setNodoRepartidor(nodoRepartidor);
            
            // Calcular ruta: Repartidor → Restaurante
            RutaOptimaDTO rutaPickup = dijkstraService.encontrarRutaOptima(
                nodoRepartidor.getId(),
                restaurante.getId(),
                true
            );
            
            if (rutaPickup == null) {
                log.error("❌ No se pudo calcular ruta repartidor → restaurante");
                pedido.setEstado(Pedido.EstadoPedido.CANCELADO);
                pedidoRepository.save(pedido);
                return CompletableFuture.completedFuture(null);
            }
            
            log.info("✅ RUTA 1 (Pickup): {:.2f} km en {} minutos", 
                    rutaPickup.getDistanciaTotalKm(), 
                    rutaPickup.getTiempoEstimadoMinutos());
            
            // ================== PASO 4: CALCULAR RUTA 2 (Restaurante → Cliente) ==================
            log.info("🗺️ PASO 3B: Calculando RUTA 2 - Restaurante → Cliente (DELIVERY)...");
            
            // Encontrar nodo más cercano al cliente
            Graph nodoCliente = lugarService.encontrarNodoMasCercano(
                pedido.getLatDestino(), 
                pedido.getLonDestino()
            );
            
            if (nodoCliente == null) {
                log.error("❌ No se encontró nodo cercano al cliente");
                pedido.setEstado(Pedido.EstadoPedido.CANCELADO);
                pedidoRepository.save(pedido);
                return CompletableFuture.completedFuture(null);
            }
            
            pedido.setNodoCliente(nodoCliente);
            
            // Calcular ruta: Restaurante → Cliente
            RutaOptimaDTO rutaDelivery = dijkstraService.encontrarRutaOptima(
                restaurante.getId(),
                nodoCliente.getId(),
                true
            );
            
            if (rutaDelivery == null) {
                log.error("❌ No se pudo calcular ruta restaurante → cliente");
                pedido.setEstado(Pedido.EstadoPedido.CANCELADO);
                pedidoRepository.save(pedido);
                return CompletableFuture.completedFuture(null);
            }
            
            log.info("✅ RUTA 2 (Delivery): {:.2f} km en {} minutos", 
                    rutaDelivery.getDistanciaTotalKm(), 
                    rutaDelivery.getTiempoEstimadoMinutos());
            
            // ================== PASO 5: ACTUALIZAR PEDIDO ==================
            log.info("📄 PASO 4: Actualizando información del pedido...");
            
            // Distancia total = ruta1 + ruta2
            double distanciaTotal = rutaPickup.getDistanciaTotalKm() + rutaDelivery.getDistanciaTotalKm();
            
            // Tiempo total = ruta1 + preparación + ruta2
            int tiempoTotal = rutaPickup.getTiempoEstimadoMinutos() + 
                             TIEMPO_PREPARACION_MIN + 
                             rutaDelivery.getTiempoEstimadoMinutos();
            
            pedido.setDistanciaKm(distanciaTotal);
            pedido.setCosto(5000.0 + (distanciaTotal * 2000.0));
            pedido.setEstado(Pedido.EstadoPedido.ASIGNADO);
            pedido.setFechaAsignacion(LocalDateTime.now());
            
            Pedido pedidoActualizado = pedidoRepository.save(pedido);
            
            // Marcar repartidor como no disponible
            repartidor.setDisponible(false);
            usuarioRepository.save(repartidor);
            
            long tiempoCalculo = System.currentTimeMillis() - tiempoInicio;
            
            // ================== PASO 6: GUARDAR HISTORIAL (2 RUTAS) ==================
            log.info("💾 PASO 5: Guardando historial de las 2 rutas...");
            
            try {
                // HISTORIAL 1: Repartidor → Restaurante (PICKUP)
                historialRutaService.guardarHistorial(
                    pedidoActualizado.getId(),
                    rutaPickup,
                    restaurante,
                    nodoCliente, // Para referencia
                    nodoRepartidor,
                    repartidor,
                    HistorialRuta.TipoCalculo.RUTA_PICKUP,
                    tiempoCalculo
                );
                log.info("✅ Historial PICKUP guardado");
                
                // HISTORIAL 2: Restaurante → Cliente (DELIVERY)
                historialRutaService.guardarHistorial(
                    pedidoActualizado.getId(),
                    rutaDelivery,
                    restaurante,
                    nodoCliente,
                    nodoRepartidor,
                    repartidor,
                    HistorialRuta.TipoCalculo.RUTA_DELIVERY,
                    tiempoCalculo
                );
                log.info("✅ Historial DELIVERY guardado");
                
            } catch (Exception e) {
                log.error("⚠️ Error guardando historial (no crítico): {}", e.getMessage());
            }
            
            // ================== RESUMEN ==================
            log.info("✅ ====== ASIGNACIÓN COMPLETADA EN {}ms ======", tiempoCalculo);
            log.info("📊 RESUMEN COMPLETO:");
            log.info("   🍽️  Restaurante: {}", restaurante.getNombre());
            log.info("   🚴 Repartidor: {}", repartidor.getNombre());
            log.info("");
            log.info("   📍 RUTA 1 (PICKUP): Repartidor → Restaurante");
            log.info("      ├─ Distancia: {:.2f} km", rutaPickup.getDistanciaTotalKm());
            log.info("      └─ Tiempo: {} minutos", rutaPickup.getTiempoEstimadoMinutos());
            log.info("");
            log.info("   ⏱️  PREPARACIÓN: {} minutos", TIEMPO_PREPARACION_MIN);
            log.info("");
            log.info("   📍 RUTA 2 (DELIVERY): Restaurante → Cliente");
            log.info("      ├─ Distancia: {:.2f} km", rutaDelivery.getDistanciaTotalKm());
            log.info("      └─ Tiempo: {} minutos", rutaDelivery.getTiempoEstimadoMinutos());
            log.info("");
            log.info("   📊 TOTALES:");
            log.info("      ├─ Distancia total: {:.2f} km", distanciaTotal);
            log.info("      ├─ Tiempo total: {} minutos", tiempoTotal);
            log.info("      └─ Costo: ${:.0f}", pedido.getCosto());
            log.info("========================================");
            
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
        
        List<Graph> restaurantes = graphRepository.findByTipo(Graph.TipoNodo.RESTAURANTE);
        
        if (restaurantes.isEmpty()) {
            log.warn("⚠️ No hay restaurantes disponibles en la BD");
            return null;
        }
        
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
        
        Usuario repartidorMasCercano = null;
        double distanciaMinima = Double.MAX_VALUE;
        
        for (Usuario rep : repartidoresDisponibles) {
            if (rep.getLatitud() == null || rep.getLongitud() == null) {
                rep.setLatitud(5.73);
                rep.setLongitud(-72.93);
            }
            
            double distancia = calcularDistancia(lat, lon, rep.getLatitud(), rep.getLongitud());
            
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
     * 📊 Obtiene estadísticas de restaurantes
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
     * 🧮 Calcula distancia entre dos coordenadas (Haversine)
     */
    private double calcularDistancia(Double lat1, Double lon1, Double lat2, Double lon2) {
        final double R = 6371;
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }



 /**
 * 🎯 ASIGNACIÓN PARA CLIENTES (NUEVO)
 * Recibe SOLO ubicación del cliente
 * Busca: Restaurante → Repartidor → Calcula rutas
 */
@Async("taskExecutor")
public CompletableFuture<Void> asignarPedidoClienteAsync(
        Long pedidoId, 
        Double latCliente, 
        Double lonCliente) {
    
    // 🚨 LOGS DE DIAGNÓSTICO CRÍTICOS
    log.info("🚨🚨🚨 MÉTODO ASYNC EJECUTÁNDOSE - Thread: {}", Thread.currentThread().getName());
    log.info("🎯 INICIANDO ASIGNACIÓN PARA CLIENTE - Pedido: {}", pedidoId);
    log.info("📍 Cliente ubicado en: ({}, {})", latCliente, lonCliente);
    
    try {
        // ✅ Llamar al método con la lógica transaccional
        procesarAsignacionCliente(pedidoId, latCliente, lonCliente);
    } catch (Exception e) {
        log.error("❌ ERROR EN ASIGNACIÓN: {}", e.getMessage(), e);
    }
    
    return CompletableFuture.completedFuture(null);
}

/**
 * 🔧 MÉTODO PRIVADO CON LÓGICA TRANSACCIONAL
 * Separado del método @Async para evitar conflictos de proxy
 */
@Transactional
private void procesarAsignacionCliente(Long pedidoId, Double latCliente, Double lonCliente) {
    
    Pedido pedido = pedidoRepository.findById(pedidoId)
            .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
    
    // ✅ PASO 1: Buscar restaurante más cercano al cliente
    log.info("🍽️ PASO 1: Buscando restaurante más cercano...");
    Graph restaurante = encontrarRestauranteMasCercano(latCliente, lonCliente);
    
    if (restaurante == null) {
        log.error("❌ No hay restaurantes disponibles");
        pedido.setEstado(Pedido.EstadoPedido.CANCELADO);
        pedidoRepository.save(pedido);
        return;
    }
    
    pedido.setRestaurante(restaurante);
    pedido.setLatDestino(restaurante.getLatitud());
    pedido.setLonDestino(restaurante.getLongitud());
    pedido.setDireccionDestino(restaurante.getNombre());
    pedidoRepository.save(pedido);
    
    log.info("✅ Restaurante asignado: {} (lat: {}, lon: {})", 
            restaurante.getNombre(), 
            restaurante.getLatitud(), 
            restaurante.getLongitud());
    
    // ✅ PASO 2: Buscar repartidor más cercano
    log.info("🚴 PASO 2: Buscando repartidor más cercano...");
    Usuario repartidor = encontrarRepartidorMasCercano(
        restaurante.getLatitud(), 
        restaurante.getLongitud()
    );
    
    if (repartidor == null) {
        log.error("❌ No hay repartidores disponibles");
        pedido.setEstado(Pedido.EstadoPedido.CANCELADO);
        pedidoRepository.save(pedido);
        return;
    }
    
    pedido.setRepartidor(repartidor);
    repartidor.setDisponible(false);
    usuarioRepository.save(repartidor);
    
    log.info("✅ Repartidor asignado: {}", repartidor.getNombre());
    
    // ✅ PASO 3: Encontrar nodos para calcular rutas
    log.info("🗺️ PASO 3: Encontrando nodos para rutas...");
    
    Graph nodoRepartidor = lugarService.encontrarNodoMasCercano(
        repartidor.getLatitud(), 
        repartidor.getLongitud()
    );
    
    Graph nodoRestaurante = restaurante; // El restaurante es un nodo
    
    Graph nodoCliente = lugarService.encontrarNodoMasCercano(
        latCliente, 
        lonCliente
    );
    
    if (nodoRepartidor == null || nodoCliente == null) {
        log.error("❌ No se encontraron nodos para las rutas");
        pedido.setEstado(Pedido.EstadoPedido.CANCELADO);
        pedidoRepository.save(pedido);
        return;
    }
    
    pedido.setNodoRepartidor(nodoRepartidor);
    pedido.setNodoCliente(nodoCliente);
    
    log.info("✅ Nodos encontrados");
    
    // ✅ PASO 4: Calcular RUTA 1 (Repartidor → Restaurante)
    log.info("🗺️ PASO 4A: Calculando RUTA 1 - Repartidor → Restaurante (PICKUP)...");
    
    RutaOptimaDTO rutaPickup = dijkstraService.encontrarRutaOptima(
        nodoRepartidor.getId(),
        restaurante.getId(),
        true
    );
    
    if (rutaPickup == null) {
        log.error("❌ No se pudo calcular ruta pickup");
        pedido.setEstado(Pedido.EstadoPedido.CANCELADO);
        pedidoRepository.save(pedido);
        return;
    }
    
    log.info("✅ RUTA 1 (Pickup): {} km en {} minutos", 
            rutaPickup.getDistanciaTotalKm(), 
            rutaPickup.getTiempoEstimadoMinutos());
    
    // ✅ PASO 5: Calcular RUTA 2 (Restaurante → Cliente)
    log.info("🗺️ PASO 4B: Calculando RUTA 2 - Restaurante → Cliente (DELIVERY)...");
    
    RutaOptimaDTO rutaDelivery = dijkstraService.encontrarRutaOptima(
        restaurante.getId(),
        nodoCliente.getId(),
        true
    );
    
    if (rutaDelivery == null) {
        log.error("❌ No se pudo calcular ruta delivery");
        pedido.setEstado(Pedido.EstadoPedido.CANCELADO);
        pedidoRepository.save(pedido);
        return;
    }
    
    log.info("✅ RUTA 2 (Delivery): {} km en {} minutos", 
            rutaDelivery.getDistanciaTotalKm(), 
            rutaDelivery.getTiempoEstimadoMinutos());
    
    // ✅ PASO 6: Actualizar pedido con información completa
    log.info("📝 PASO 5: Actualizando información del pedido...");
    
    double distanciaTotal = rutaPickup.getDistanciaTotalKm() + rutaDelivery.getDistanciaTotalKm();
    int tiempoTotal = rutaPickup.getTiempoEstimadoMinutos() + 10 + rutaDelivery.getTiempoEstimadoMinutos();
    
    pedido.setDistanciaKm(distanciaTotal);
    pedido.setCosto(5000.0 + (distanciaTotal * 2000.0));
    pedido.setEstado(Pedido.EstadoPedido.ASIGNADO);
    pedido.setFechaAsignacion(LocalDateTime.now());
    pedido.setDireccionOrigen("Ubicación del cliente");
    
    Pedido pedidoActualizado = pedidoRepository.save(pedido);
    
    log.info("✅ Pedido actualizado");
    log.info("📊 RESUMEN COMPLETO:");
    log.info("   🍽️  Restaurante: {}", restaurante.getNombre());
    log.info("   🚴 Repartidor: {}", repartidor.getNombre());
    log.info("   📍 Distancia total: {} km", distanciaTotal);
    log.info("   ⏱️  Tiempo total: {} minutos", tiempoTotal);
    log.info("   💰 Costo: ${}", pedido.getCosto());
    
    // ✅ PASO 7: Guardar historial de rutas
    log.info("💾 PASO 6: Guardando historial de rutas...");
    
    try {
        historialRutaService.guardarHistorial(
            pedidoActualizado.getId(),
            rutaPickup,
            restaurante,
            nodoCliente,
            nodoRepartidor,
            repartidor,
            HistorialRuta.TipoCalculo.RUTA_PICKUP,
            0L
        );
        
        historialRutaService.guardarHistorial(
            pedidoActualizado.getId(),
            rutaDelivery,
            restaurante,
            nodoCliente,
            nodoRepartidor,
            repartidor,
            HistorialRuta.TipoCalculo.RUTA_DELIVERY,
            0L
        );
        
        log.info("✅ Historial guardado");
    } catch (Exception e) {
        log.error("⚠️ Error guardando historial (no crítico): {}", e.getMessage());
    }
    
    log.info("✅ ====== ASIGNACIÓN COMPLETADA ======");
}
}