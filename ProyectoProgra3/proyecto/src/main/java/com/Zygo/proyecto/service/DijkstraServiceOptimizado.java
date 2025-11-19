package com.Zygo.proyecto.service;

import com.Zygo.proyecto.model.Graph;
import com.Zygo.proyecto.model.Edge;
import com.Zygo.proyecto.dto.RutaOptimaDTO;
import com.Zygo.proyecto.repository.GraphRepository;
import com.Zygo.proyecto.repository.EdgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🚀 Servicio OPTIMIZADO de Dijkstra
 * Mejoras implementadas:
 * - Cache de rutas calculadas
 * - Carga selectiva de nodos (solo alcanzables)
 * - Map pre-cargado de adyacencias
 * - Early termination
 * - PriorityQueue optimizada
 */
@Service
public class DijkstraServiceOptimizado {
    
    private static final Logger log = LoggerFactory.getLogger(DijkstraServiceOptimizado.class);
    private static final double MAX_DISTANCIA_BUSQUEDA_KM = 50.0; // Radio máximo de búsqueda
    
    @Autowired
    private GraphRepository graphRepository;
    
    @Autowired
    private EdgeRepository edgeRepository;
    
    /**
     * 🎯 MÉTODO PRINCIPAL OPTIMIZADO
     * Usa @Cacheable para evitar recalcular rutas idénticas
     */
    @Cacheable(value = "rutas-calculadas", 
               key = "#origenId + '-' + #destinoId + '-' + #considerarTrafico",
               unless = "#result == null")
    @Transactional(readOnly = true)
    public RutaOptimaDTO encontrarRutaOptima(Long origenId, Long destinoId, boolean considerarTrafico) {
        long inicio = System.currentTimeMillis();
        log.info("🔍 Calculando ruta óptima: {} → {} (tráfico: {})", origenId, destinoId, considerarTrafico);
        
        // Validación rápida
        if (origenId.equals(destinoId)) {
            throw new RuntimeException("Origen y destino son iguales");
        }
        
        Graph nodoOrigen = graphRepository.findById(origenId)
                .orElseThrow(() -> new RuntimeException("Nodo origen no encontrado"));
        Graph nodoDestino = graphRepository.findById(destinoId)
                .orElseThrow(() -> new RuntimeException("Nodo destino no encontrado"));
        
        // 🚀 OPTIMIZACIÓN 1: Pre-cargar SOLO nodos cercanos, no todo el grafo
        Set<Long> nodosRelevantes = obtenerNodosRelevantes(nodoOrigen, nodoDestino);
        Map<Long, Graph> nodosMap = graphRepository.findAllById(nodosRelevantes)
                .stream()
                .collect(Collectors.toMap(Graph::getId, g -> g));
        
        log.info("📊 Nodos relevantes cargados: {} (vs {} totales)", nodosMap.size(), graphRepository.count());
        
        // 🚀 OPTIMIZACIÓN 2: Pre-cargar TODAS las aristas en un solo query
        Map<Long, List<AristaInfo>> grafosAdyacencia = construirGrafoAdyacencia(nodosRelevantes, considerarTrafico);
        
        // Ejecutar Dijkstra optimizado
        ResultadoDijkstra resultado = ejecutarDijkstraOptimizado(
            origenId, destinoId, nodosMap, grafosAdyacencia
        );
        
        if (resultado == null || resultado.distancias.get(destinoId) == Double.POSITIVE_INFINITY) {
            throw new RuntimeException("No existe camino entre los nodos especificados");
        }
        
        // Reconstruir camino
        List<Graph> camino = reconstruirCamino(resultado.predecesores, nodosMap, origenId, destinoId);
        
        // Construir DTO
        RutaOptimaDTO dto = construirRutaDTO(camino, resultado.distancias.get(destinoId), considerarTrafico);
        
        long duracion = System.currentTimeMillis() - inicio;
        log.info("✅ Ruta calculada en {}ms - Distancia: {:.2f}km, Nodos: {}", 
                 duracion, dto.getDistanciaTotalKm(), camino.size());
        
        return dto;
    }
    
    /**
     * 🚀 OPTIMIZACIÓN 3: Obtener solo nodos dentro de un radio razonable
     */
    private Set<Long> obtenerNodosRelevantes(Graph origen, Graph destino) {
        Set<Long> relevantes = new HashSet<>();
        
        // Calcular punto medio y radio
        double latCentro = (origen.getLatitud() + destino.getLatitud()) / 2;
        double lonCentro = (origen.getLongitud() + destino.getLongitud()) / 2;
        double distanciaOD = origen.calcularDistancia(destino);
        double radio = Math.max(distanciaOD * 1.5, 5.0); // 150% de la distancia directa, mínimo 5km
        
        // Buscar nodos cercanos al área de interés
        List<Graph> nodosCercanos = graphRepository.findNodosCercanos(latCentro, lonCentro, radio);
        nodosCercanos.forEach(n -> relevantes.add(n.getId()));
        
        // Siempre incluir origen y destino
        relevantes.add(origen.getId());
        relevantes.add(destino.getId());
        
        return relevantes;
    }
    
    /**
     * 🚀 OPTIMIZACIÓN 4: Construir grafo de adyacencia en memoria (UNA SOLA QUERY)
     */
    private Map<Long, List<AristaInfo>> construirGrafoAdyacencia(Set<Long> nodosRelevantes, boolean considerarTrafico) {
        Map<Long, List<AristaInfo>> adyacencia = new HashMap<>();
        
        // Inicializar listas vacías
        nodosRelevantes.forEach(id -> adyacencia.put(id, new ArrayList<>()));
        
        // 🔥 UNA SOLA QUERY para todas las aristas relevantes
        List<Edge> aristasRelevantes = edgeRepository.findAristasPorNodos(new ArrayList<>(nodosRelevantes));
        
        // Construir mapa de adyacencia
        for (Edge arista : aristasRelevantes) {
            if (!arista.getActivo()) continue;
            
            Long origenId = arista.getNodoOrigen().getId();
            Long destinoId = arista.getNodoDestino().getId();
            double peso = considerarTrafico ? arista.getPesoConTrafico() : arista.getDistanciaKm();
            
            // Agregar arista origen -> destino
            if (adyacencia.containsKey(origenId)) {
                adyacencia.get(origenId).add(new AristaInfo(destinoId, peso, arista));
            }
            
            // Si es bidireccional, agregar destino -> origen
            if (arista.getEsBidireccional() && adyacencia.containsKey(destinoId)) {
                adyacencia.get(destinoId).add(new AristaInfo(origenId, peso, arista));
            }
        }
        
        return adyacencia;
    }
    
    /**
     * 🚀 ALGORITMO DE DIJKSTRA OPTIMIZADO
     * Mejoras:
     * - Early termination (para cuando encuentra el destino)
     * - Sin cargar todo el grafo
     * - Sin consultas N+1
     */
    private ResultadoDijkstra ejecutarDijkstraOptimizado(
            Long origenId, 
            Long destinoId,
            Map<Long, Graph> nodos,
            Map<Long, List<AristaInfo>> adyacencia) {
        
        Map<Long, Double> distancias = new HashMap<>();
        Map<Long, Long> predecesores = new HashMap<>();
        PriorityQueue<NodoDistancia> cola = new PriorityQueue<>();
        Set<Long> visitados = new HashSet<>();
        
        // Inicializar
        for (Long nodoId : nodos.keySet()) {
            distancias.put(nodoId, Double.POSITIVE_INFINITY);
        }
        distancias.put(origenId, 0.0);
        cola.offer(new NodoDistancia(origenId, 0.0));
        
        // Procesar nodos
        while (!cola.isEmpty()) {
            NodoDistancia actual = cola.poll();
            Long nodoActualId = actual.nodoId;
            
            // Ya visitado
            if (visitados.contains(nodoActualId)) continue;
            visitados.add(nodoActualId);
            
            // 🎯 EARLY TERMINATION: Si llegamos al destino, terminamos
            if (nodoActualId.equals(destinoId)) {
                log.debug("🎯 Destino alcanzado. Visitados: {}/{}", visitados.size(), nodos.size());
                break;
            }
            
            // Procesar vecinos
            List<AristaInfo> vecinos = adyacencia.getOrDefault(nodoActualId, Collections.emptyList());
            for (AristaInfo aristaInfo : vecinos) {
                Long vecinoId = aristaInfo.destinoId;
                
                if (visitados.contains(vecinoId)) continue;
                
                double nuevaDistancia = distancias.get(nodoActualId) + aristaInfo.peso;
                
                if (nuevaDistancia < distancias.get(vecinoId)) {
                    distancias.put(vecinoId, nuevaDistancia);
                    predecesores.put(vecinoId, nodoActualId);
                    cola.offer(new NodoDistancia(vecinoId, nuevaDistancia));
                }
            }
        }
        
        ResultadoDijkstra resultado = new ResultadoDijkstra();
        resultado.distancias = distancias;
        resultado.predecesores = predecesores;
        return resultado;
    }
    
    /**
     * Reconstruir camino desde predecesores
     */
    private List<Graph> reconstruirCamino(Map<Long, Long> predecesores, Map<Long, Graph> nodos, 
                                          Long origenId, Long destinoId) {
        List<Graph> camino = new ArrayList<>();
        Long actual = destinoId;
        
        while (actual != null) {
            camino.add(0, nodos.get(actual));
            actual = predecesores.get(actual);
            
            // Evitar bucles infinitos
            if (camino.size() > nodos.size()) {
                throw new RuntimeException("Error: bucle detectado en reconstrucción de camino");
            }
        }
        
        // Verificar que el camino empieza en el origen
        if (camino.isEmpty() || !camino.get(0).getId().equals(origenId)) {
            return new ArrayList<>(); // Camino no válido
        }
        
        return camino;
    }
    
    /**
     * Construir DTO de respuesta
     */
    private RutaOptimaDTO construirRutaDTO(List<Graph> camino, double distanciaTotal, boolean considerarTrafico) {
        RutaOptimaDTO dto = new RutaOptimaDTO();
        
        dto.setNodos(camino.stream()
                .map(this::convertirNodoADTO)
                .collect(Collectors.toList()));
        
        dto.setDistanciaTotalKm(distanciaTotal);
        
        // Calcular segmentos y tiempo
        List<RutaOptimaDTO.SegmentoRuta> segmentos = new ArrayList<>();
        int tiempoTotal = 0;
        
        for (int i = 0; i < camino.size() - 1; i++) {
            Graph actual = camino.get(i);
            Graph siguiente = camino.get(i + 1);
            
            // Buscar arista
            Optional<Edge> aristaOpt = edgeRepository
                    .findByNodoOrigenAndNodoDestinoAndActivoTrue(actual, siguiente);
            
            if (!aristaOpt.isPresent()) {
                aristaOpt = edgeRepository
                        .findByNodoOrigenAndNodoDestinoAndActivoTrue(siguiente, actual);
            }
            
            if (aristaOpt.isPresent()) {
                Edge arista = aristaOpt.get();
                RutaOptimaDTO.SegmentoRuta segmento = new RutaOptimaDTO.SegmentoRuta();
                segmento.setNodoOrigenId(actual.getId());
                segmento.setNodoDestinoId(siguiente.getId());
                segmento.setDistanciaKm(arista.getDistanciaKm());
                segmento.setTiempoEstimadoMinutos(considerarTrafico ? 
                        arista.getTiempoConTrafico() : arista.getTiempoEstimadoMinutos());
                segmento.setNombreCalle(arista.getNombreCalle());
                segmentos.add(segmento);
                
                tiempoTotal += segmento.getTiempoEstimadoMinutos();
            }
        }
        
        dto.setSegmentos(segmentos);
        dto.setTiempoEstimadoMinutos(tiempoTotal);
        dto.setConsiderandoTrafico(considerarTrafico);
        dto.setInstrucciones(generarInstrucciones(camino, segmentos));
        
        return dto;
    }
    
    private RutaOptimaDTO.NodoDTO convertirNodoADTO(Graph nodo) {
        RutaOptimaDTO.NodoDTO dto = new RutaOptimaDTO.NodoDTO();
        dto.setId(nodo.getId());
        dto.setNombre(nodo.getNombre());
        dto.setLatitud(nodo.getLatitud());
        dto.setLongitud(nodo.getLongitud());
        dto.setTipo(nodo.getTipo().toString());
        dto.setDireccion(nodo.getDireccionCompleta());
        return dto;
    }
    
    private List<String> generarInstrucciones(List<Graph> camino, List<RutaOptimaDTO.SegmentoRuta> segmentos) {
        List<String> instrucciones = new ArrayList<>();
        
        if (camino.isEmpty()) return instrucciones;
        
        instrucciones.add("Inicio en: " + camino.get(0).getNombre());
        
        for (int i = 0; i < segmentos.size(); i++) {
            RutaOptimaDTO.SegmentoRuta segmento = segmentos.get(i);
            String instruccion = String.format("Continúa por %s durante %.2f km (%d min)",
                    segmento.getNombreCalle() != null ? segmento.getNombreCalle() : "la calle",
                    segmento.getDistanciaKm(),
                    segmento.getTiempoEstimadoMinutos());
            instrucciones.add(instruccion);
        }
        
        instrucciones.add("Llegada a destino: " + camino.get(camino.size() - 1).getNombre());
        
        return instrucciones;
    }
    
    // ==================== CLASES AUXILIARES ====================
    
    private static class AristaInfo {
        Long destinoId;
        double peso;
        Edge arista;
        
        AristaInfo(Long destinoId, double peso, Edge arista) {
            this.destinoId = destinoId;
            this.peso = peso;
            this.arista = arista;
        }
    }
    
    private static class NodoDistancia implements Comparable<NodoDistancia> {
        Long nodoId;
        double distancia;
        
        NodoDistancia(Long nodoId, double distancia) {
            this.nodoId = nodoId;
            this.distancia = distancia;
        }
        
        @Override
        public int compareTo(NodoDistancia otro) {
            return Double.compare(this.distancia, otro.distancia);
        }
    }
    
    private static class ResultadoDijkstra {
        Map<Long, Double> distancias;
        Map<Long, Long> predecesores;
    }
}