package com.Zygo.proyecto.service;

import com.Zygo.proyecto.dto.HistorialRutaDTO;
import com.Zygo.proyecto.dto.RutaOptimaDTO;
import com.Zygo.proyecto.model.*;
import com.Zygo.proyecto.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 📊 SERVICIO DE HISTORIAL DE RUTAS
 * 
 * Responsabilidades:
 * - Guardar historial de cada cálculo de ruta
 * - Consultar historial por pedido/repartidor/admin
 * - Recrear rutas desde el historial
 * - Generar estadísticas
 */
@Service
public class HistorialRutaService {
    
    private static final Logger log = LoggerFactory.getLogger(HistorialRutaService.class);
    
    @Autowired
    private HistorialRutaRepository historialRepository;
    
    @Autowired
    private PedidoRepository pedidoRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private GraphRepository graphRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 💾 GUARDAR HISTORIAL DE RUTA CALCULADA
     * 
     * Este método se llama automáticamente cuando se calcula una ruta
     */
    @Transactional
    public HistorialRuta guardarHistorial(
            Long pedidoId,
            RutaOptimaDTO rutaOptima,
            Graph restaurante,
            Graph nodoCliente,
            Graph nodoRepartidor,
            Usuario repartidor,
            HistorialRuta.TipoCalculo tipoCalculo,
            Long tiempoCalculoMs) {
        
        log.info("💾 Guardando historial para pedido {}", pedidoId);
        
        try {
            Pedido pedido = pedidoRepository.findById(pedidoId)
                    .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
            
            HistorialRuta historial = new HistorialRuta();
            
            // Relaciones
            historial.setPedido(pedido);
            historial.setRestaurante(restaurante);
            historial.setNodoCliente(nodoCliente);
            historial.setNodoRepartidor(nodoRepartidor);
            historial.setRepartidor(repartidor);
            
            // Información del cálculo
            historial.setTipoCalculo(tipoCalculo);
            historial.setFechaCalculo(LocalDateTime.now());
            historial.setTiempoCalculoMs(tiempoCalculoMs);
            
            // Datos de la ruta
            historial.setDistanciaTotalKm(rutaOptima.getDistanciaTotalKm());
            historial.setTiempoEstimadoMin(rutaOptima.getTiempoEstimadoMinutos());
            historial.setCostoCalculado(rutaOptima.getCostoEstimado());
            historial.setConsideroTrafico(rutaOptima.getConsiderandoTrafico());
            
            // Serializar ruta completa a JSON
            historial.setNodosRuta(objectMapper.writeValueAsString(rutaOptima.getNodos()));
            historial.setSegmentosRuta(objectMapper.writeValueAsString(rutaOptima.getSegmentos()));
            
            // Instrucciones
            if (rutaOptima.getInstrucciones() != null && !rutaOptima.getInstrucciones().isEmpty()) {
                historial.setInstrucciones(String.join("\n", rutaOptima.getInstrucciones()));
            }
            
            // Visibilidad por defecto
            historial.setVisibilidad(HistorialRuta.VisibilidadHistorial.ADMIN_Y_REPARTIDOR);
            historial.setActivo(true);
            
            HistorialRuta guardado = historialRepository.save(historial);
            
            log.info("✅ Historial guardado con ID: {} - {}km, {}min", 
                    guardado.getId(), 
                    guardado.getDistanciaTotalKm(), 
                    guardado.getTiempoEstimadoMin());
            
            return guardado;
            
        } catch (Exception e) {
            log.error("❌ Error guardando historial: {}", e.getMessage(), e);
            throw new RuntimeException("Error al guardar historial: " + e.getMessage());
        }
    }
    
    /**
     * 🔍 OBTENER HISTORIAL POR PEDIDO
     */
    @Transactional(readOnly = true)
    public List<HistorialRutaDTO> obtenerHistorialPorPedido(Long pedidoId) {
        log.info("🔍 Buscando historial para pedido {}", pedidoId);
        
        List<HistorialRuta> historiales = historialRepository
                .findByPedidoIdAndActivoTrue(pedidoId);
        
        return historiales.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 📋 OBTENER HISTORIAL COMPLETO (ADMIN)
     */
    @Transactional(readOnly = true)
    public List<HistorialRutaDTO> obtenerHistorialCompleto(int limite) {
        log.info("📋 Obteniendo historial completo (límite: {})", limite);
        
        List<HistorialRuta> historiales = historialRepository
                .findTop50ByActivoTrueOrderByFechaCalculoDesc();
        
        return historiales.stream()
                .limit(limite)
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 🚴 OBTENER HISTORIAL POR REPARTIDOR
     */
    @Transactional(readOnly = true)
    public List<HistorialRutaDTO> obtenerHistorialPorRepartidor(Long repartidorId) {
        log.info("🚴 Obteniendo historial del repartidor {}", repartidorId);
        
        Usuario repartidor = usuarioRepository.findById(repartidorId)
                .orElseThrow(() -> new RuntimeException("Repartidor no encontrado"));
        
        List<HistorialRuta> historiales = historialRepository
                .findByRepartidorAndActivoTrueOrderByFechaCalculoDesc(repartidor);
        
        return historiales.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 🔄 RECREAR RUTA DESDE HISTORIAL
     * 
     * Convierte el historial guardado de vuelta a RutaOptimaDTO
     * para poder visualizarlo en el mapa
     */
    @Transactional(readOnly = true)
    public RutaOptimaDTO recrearRutaDesdeHistorial(Long historialId) {
        log.info("🔄 Recreando ruta desde historial {}", historialId);
        
        HistorialRuta historial = historialRepository.findById(historialId)
                .orElseThrow(() -> new RuntimeException("Historial no encontrado"));
        
        try {
            RutaOptimaDTO ruta = new RutaOptimaDTO();
            
            // Deserializar nodos
            if (historial.getNodosRuta() != null) {
                List<RutaOptimaDTO.NodoDTO> nodos = objectMapper.readValue(
                    historial.getNodosRuta(),
                    objectMapper.getTypeFactory().constructCollectionType(
                        List.class, RutaOptimaDTO.NodoDTO.class
                    )
                );
                ruta.setNodos(nodos);
            }
            
            // Deserializar segmentos
            if (historial.getSegmentosRuta() != null) {
                List<RutaOptimaDTO.SegmentoRuta> segmentos = objectMapper.readValue(
                    historial.getSegmentosRuta(),
                    objectMapper.getTypeFactory().constructCollectionType(
                        List.class, RutaOptimaDTO.SegmentoRuta.class
                    )
                );
                ruta.setSegmentos(segmentos);
            }
            
            // Datos básicos
            ruta.setDistanciaTotalKm(historial.getDistanciaTotalKm());
            ruta.setTiempoEstimadoMinutos(historial.getTiempoEstimadoMin());
            ruta.setCostoEstimado(historial.getCostoCalculado());
            ruta.setConsiderandoTrafico(historial.getConsideroTrafico());
            
            // Instrucciones
            if (historial.getInstrucciones() != null) {
                ruta.setInstrucciones(Arrays.asList(historial.getInstrucciones().split("\n")));
            }
            
            log.info("✅ Ruta recreada exitosamente: {}km, {} nodos", 
                    ruta.getDistanciaTotalKm(), 
                    ruta.getNodos() != null ? ruta.getNodos().size() : 0);
            
            return ruta;
            
        } catch (Exception e) {
            log.error("❌ Error recreando ruta: {}", e.getMessage());
            throw new RuntimeException("Error al recrear ruta: " + e.getMessage());
        }
    }
    
    /**
     * 📊 OBTENER ESTADÍSTICAS
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadisticas() {
        log.info("📊 Generando estadísticas de historial");
        
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Total de rutas calculadas
            long totalRutas = historialRepository.count();
            stats.put("totalRutas", totalRutas);
            
            // Rutas calculadas hoy
            long rutasHoy = historialRepository.countRutasCalculadasHoy();
            stats.put("rutasHoy", rutasHoy);
            
            // Últimas 10 rutas
            List<HistorialRuta> ultimasRutas = historialRepository
                    .findTop50ByActivoTrueOrderByFechaCalculoDesc()
                    .stream()
                    .limit(10)
                    .collect(Collectors.toList());
            
            stats.put("ultimasRutas", ultimasRutas.stream()
                    .map(h -> Map.of(
                        "id", h.getId(),
                        "pedidoId", h.getPedido().getId(),
                        "fecha", h.getFechaCalculo(),
                        "distanciaKm", h.getDistanciaTotalKm(),
                        "tiempoMin", h.getTiempoEstimadoMin()
                    ))
                    .collect(Collectors.toList()));
            
            // Distribución por tipo de cálculo
            Map<String, Long> porTipo = new HashMap<>();
            for (HistorialRuta.TipoCalculo tipo : HistorialRuta.TipoCalculo.values()) {
                long count = historialRepository
                        .findByTipoCalculoAndActivoTrueOrderByFechaCalculoDesc(tipo)
                        .size();
                porTipo.put(tipo.name(), count);
            }
            stats.put("distribucionPorTipo", porTipo);
            
        } catch (Exception e) {
            log.error("❌ Error generando estadísticas: {}", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 🔧 CONVERTIR ENTIDAD A DTO
     */
    private HistorialRutaDTO convertirADTO(HistorialRuta historial) {
        HistorialRutaDTO dto = new HistorialRutaDTO();
        
        // IDs y básicos
        dto.setId(historial.getId());
        dto.setPedidoId(historial.getPedido().getId());
        dto.setTipoPedido(historial.getPedido().getDescripcion());
        
        // Restaurante
        if (historial.getRestaurante() != null) {
            Graph rest = historial.getRestaurante();
            dto.setRestaurante(new HistorialRutaDTO.InfoRestaurante(
                rest.getId(),
                rest.getNombre(),
                rest.getLatitud(),
                rest.getLongitud()
            ));
        }
        
        // Nodo cliente
        if (historial.getNodoCliente() != null) {
            Graph nodo = historial.getNodoCliente();
            dto.setNodoCliente(new HistorialRutaDTO.InfoNodo(
                nodo.getId(),
                nodo.getNombre(),
                nodo.getLatitud(),
                nodo.getLongitud(),
                nodo.getTipo().name()
            ));
        }
        
        // Nodo repartidor
        if (historial.getNodoRepartidor() != null) {
            Graph nodo = historial.getNodoRepartidor();
            dto.setNodoRepartidor(new HistorialRutaDTO.InfoNodo(
                nodo.getId(),
                nodo.getNombre(),
                nodo.getLatitud(),
                nodo.getLongitud(),
                nodo.getTipo().name()
            ));
        }
        
        // Repartidor
        if (historial.getRepartidor() != null) {
            Usuario rep = historial.getRepartidor();
            dto.setRepartidor(new HistorialRutaDTO.InfoRepartidor(
                rep.getId(),
                rep.getNombre(),
                rep.getTelefono()
            ));
        }
        
        // Información del cálculo
        dto.setTipoCalculo(historial.getTipoCalculo().name());
        dto.setFechaCalculo(historial.getFechaCalculo());
        dto.setTiempoCalculoMs(historial.getTiempoCalculoMs());
        
        // Datos de la ruta
        dto.setDistanciaTotalKm(historial.getDistanciaTotalKm());
        dto.setTiempoEstimadoMin(historial.getTiempoEstimadoMin());
        dto.setCostoCalculado(historial.getCostoCalculado());
        dto.setConsideroTrafico(historial.getConsideroTrafico());
        
        // Deserializar nodos y segmentos para el DTO
        try {
            if (historial.getNodosRuta() != null) {
                List<RutaOptimaDTO.NodoDTO> nodos = objectMapper.readValue(
                    historial.getNodosRuta(),
                    objectMapper.getTypeFactory().constructCollectionType(
                        List.class, RutaOptimaDTO.NodoDTO.class
                    )
                );
                dto.setNodos(nodos);
            }
            
            if (historial.getSegmentosRuta() != null) {
                List<RutaOptimaDTO.SegmentoRuta> segmentos = objectMapper.readValue(
                    historial.getSegmentosRuta(),
                    objectMapper.getTypeFactory().constructCollectionType(
                        List.class, RutaOptimaDTO.SegmentoRuta.class
                    )
                );
                dto.setSegmentos(segmentos);
            }
            
            if (historial.getInstrucciones() != null) {
                dto.setInstrucciones(Arrays.asList(historial.getInstrucciones().split("\n")));
            }
        } catch (Exception e) {
            log.warn("⚠️ Error deserializando ruta del historial {}: {}", 
                    historial.getId(), e.getMessage());
        }
        
        // Metadata
        dto.setVisibilidad(historial.getVisibilidad().name());
        dto.setActivo(historial.getActivo());
        dto.setObservaciones(historial.getObservaciones());
        
        return dto;
    }
}