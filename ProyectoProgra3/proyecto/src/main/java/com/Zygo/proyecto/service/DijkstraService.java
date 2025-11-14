package com.Zygo.proyecto.service;

import com.Zygo.proyecto.model.Graph;
import com.Zygo.proyecto.model.Edge;
import com.Zygo.proyecto.dto.RutaOptimaDTO;
import com.Zygo.proyecto.repository.GraphRepository;
import com.Zygo.proyecto.repository.EdgeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio que implementa el algoritmo de Dijkstra para encontrar rutas óptimas
 */
@Service
public class DijkstraService {
    
    private static final Logger log = LoggerFactory.getLogger(DijkstraService.class);
    
    @Autowired
    private GraphRepository graphRepository;
    
    @Autowired
    private EdgeRepository edgeRepository;
    
    /**
     * Encuentra la ruta más corta entre dos nodos usando el algoritmo de Dijkstra
     * @param nodoOrigenId ID del nodo de origen
     * @param nodoDestinoId ID del nodo de destino
     * @param considerarTrafico Si debe considerar el factor de tráfico
     * @return RutaOptimaDTO con la ruta óptima encontrada
     */
    @Transactional(readOnly = true)
    public RutaOptimaDTO encontrarRutaOptima(Long nodoOrigenId, Long nodoDestinoId, boolean considerarTrafico) {
        log.info("Calculando ruta óptima de nodo {} a nodo {} (considerando tráfico: {})", 
                 nodoOrigenId, nodoDestinoId, considerarTrafico);
        
        Graph nodoOrigen = graphRepository.findById(nodoOrigenId)
                .orElseThrow(() -> new RuntimeException("Nodo origen no encontrado"));
        Graph nodoDestino = graphRepository.findById(nodoDestinoId)
                .orElseThrow(() -> new RuntimeException("Nodo destino no encontrado"));
        
        // Ejecutar algoritmo de Dijkstra
        Map<Graph, DijkstraInfo> resultado = ejecutarDijkstra(nodoOrigen, considerarTrafico);
        
        // Reconstruir el camino
        List<Graph> camino = reconstruirCamino(resultado, nodoDestino);
        
        if (camino.isEmpty()) {
            throw new RuntimeException("No existe camino entre los nodos especificados");
        }
        
        // Crear DTO de respuesta
        return construirRutaDTO(camino, resultado.get(nodoDestino), considerarTrafico);
    }
    
    /**
     * Encuentra múltiples rutas óptimas para varios pedidos (TSP simplificado)
     * @param nodoBaseId ID del nodo base (donde está el repartidor)
     * @param nodosDestino Lista de IDs de nodos destino (pedidos a entregar)
     * @return Lista de rutas óptimas ordenadas
     */
    @Transactional(readOnly = true)
    public List<RutaOptimaDTO> encontrarRutasMultiples(Long nodoBaseId, List<Long> nodosDestino) {
        log.info("Calculando rutas múltiples desde nodo base {} a {} destinos", nodoBaseId, nodosDestino.size());
        
        Graph nodoBase = graphRepository.findById(nodoBaseId)
                .orElseThrow(() -> new RuntimeException("Nodo base no encontrado"));
        
        List<Graph> destinos = graphRepository.findAllById(nodosDestino);
        
        if (destinos.size() != nodosDestino.size()) {
            throw new RuntimeException("Algunos nodos destino no fueron encontrados");
        }
        
        // Usar algoritmo del vecino más cercano como heurística
        List<RutaOptimaDTO> rutasOptimas = new ArrayList<>();
        Set<Graph> visitados = new HashSet<>();
        Graph nodoActual = nodoBase;
        
        while (visitados.size() < destinos.size()) {
            Graph siguienteNodo = encontrarNodoMasCercano(nodoActual, destinos, visitados);
            if (siguienteNodo == null) break;
            
            RutaOptimaDTO ruta = encontrarRutaOptima(nodoActual.getId(), siguienteNodo.getId(), true);
            rutasOptimas.add(ruta);
            
            visitados.add(siguienteNodo);
            nodoActual = siguienteNodo;
        }
        
        // Agregar ruta de regreso a la base si es necesario
        if (!nodoActual.equals(nodoBase) && rutasOptimas.size() == destinos.size()) {
            RutaOptimaDTO rutaRegreso = encontrarRutaOptima(nodoActual.getId(), nodoBase.getId(), true);
            rutasOptimas.add(rutaRegreso);
        }
        
        return rutasOptimas;
    }
    
    /**
     * Implementación del algoritmo de Dijkstra
     */
    private Map<Graph, DijkstraInfo> ejecutarDijkstra(Graph origen, boolean considerarTrafico) {
        Map<Graph, DijkstraInfo> info = new HashMap<>();
        PriorityQueue<NodoConDistancia> cola = new PriorityQueue<>();
        Set<Graph> visitados = new HashSet<>();
        
        // Obtener todos los nodos del grafo
        List<Graph> todosLosNodos = graphRepository.findAll();
        
        // Inicializar distancias
        for (Graph nodo : todosLosNodos) {
            if (nodo.equals(origen)) {
                info.put(nodo, new DijkstraInfo(0.0, null));
                cola.offer(new NodoConDistancia(nodo, 0.0));
            } else {
                info.put(nodo, new DijkstraInfo(Double.POSITIVE_INFINITY, null));
            }
        }
        
        // Procesar nodos
        while (!cola.isEmpty()) {
            NodoConDistancia actual = cola.poll();
            Graph nodoActual = actual.nodo;
            
            if (visitados.contains(nodoActual)) continue;
            visitados.add(nodoActual);
            
            // Obtener aristas salientes
            List<Edge> aristasSalientes = edgeRepository.findByNodoOrigenAndActivoTrue(nodoActual);
            
            for (Edge arista : aristasSalientes) {
                Graph vecino = arista.getNodoDestino();
                
                if (visitados.contains(vecino)) continue;
                
                // Calcular nueva distancia
                double peso = considerarTrafico ? arista.getPesoConTrafico() : arista.getDistanciaKm();
                double nuevaDistancia = info.get(nodoActual).distancia + peso;
                
                // Actualizar si encontramos un camino más corto
                if (nuevaDistancia < info.get(vecino).distancia) {
                    info.put(vecino, new DijkstraInfo(nuevaDistancia, nodoActual));
                    cola.offer(new NodoConDistancia(vecino, nuevaDistancia));
                }
            }
            
            // Considerar aristas bidireccionales
            List<Edge> aristasEntrantes = edgeRepository.findByNodoDestinoAndEsBidireccionalTrueAndActivoTrue(nodoActual);
            
            for (Edge arista : aristasEntrantes) {
                Graph vecino = arista.getNodoOrigen();
                
                if (visitados.contains(vecino)) continue;
                
                double peso = considerarTrafico ? arista.getPesoConTrafico() : arista.getDistanciaKm();
                double nuevaDistancia = info.get(nodoActual).distancia + peso;
                
                if (nuevaDistancia < info.get(vecino).distancia) {
                    info.put(vecino, new DijkstraInfo(nuevaDistancia, vecino));
                    cola.offer(new NodoConDistancia(vecino, nuevaDistancia));
                }
            }
        }
        
        return info;
    }
    
    /**
     * Reconstruye el camino desde el resultado de Dijkstra
     */
    private List<Graph> reconstruirCamino(Map<Graph, DijkstraInfo> resultado, Graph destino) {
        List<Graph> camino = new ArrayList<>();
        Graph actual = destino;
        
        // Verificar si existe un camino
        if (resultado.get(destino).distancia == Double.POSITIVE_INFINITY) {
            return camino; // Camino vacío indica que no hay ruta
        }
        
        // Reconstruir el camino desde el destino hasta el origen
        while (actual != null) {
            camino.add(0, actual); // Agregar al principio para mantener el orden correcto
            actual = resultado.get(actual).predecesor;
        }
        
        return camino;
    }
    
    /**
     * Construye el DTO de respuesta con la ruta óptima
     */
    private RutaOptimaDTO construirRutaDTO(List<Graph> camino, DijkstraInfo infoDestino, boolean considerarTrafico) {
        RutaOptimaDTO dto = new RutaOptimaDTO();
        
        // Información básica de la ruta
        dto.setNodos(camino.stream()
                .map(this::convertirNodoADTO)
                .collect(Collectors.toList()));
        
        dto.setDistanciaTotalKm(infoDestino.distancia);
        
        // Calcular tiempo estimado total
        int tiempoTotal = 0;
        double distanciaAcumulada = 0.0;
        List<RutaOptimaDTO.SegmentoRuta> segmentos = new ArrayList<>();
        
        for (int i = 0; i < camino.size() - 1; i++) {
            Graph nodoActual = camino.get(i);
            Graph siguienteNodo = camino.get(i + 1);
            
            // Buscar la arista entre estos nodos
            Edge arista = edgeRepository.findByNodoOrigenAndNodoDestinoAndActivoTrue(nodoActual, siguienteNodo)
                    .orElseGet(() -> edgeRepository.findByNodoOrigenAndNodoDestinoAndActivoTrue(siguienteNodo, nodoActual)
                            .orElse(null));
            
            if (arista != null) {
                RutaOptimaDTO.SegmentoRuta segmento = new RutaOptimaDTO.SegmentoRuta();
                segmento.setNodoOrigenId(nodoActual.getId());
                segmento.setNodoDestinoId(siguienteNodo.getId());
                segmento.setDistanciaKm(arista.getDistanciaKm());
                segmento.setTiempoEstimadoMinutos(considerarTrafico ? 
                        arista.getTiempoConTrafico() : arista.getTiempoEstimadoMinutos());
                segmento.setNombreCalle(arista.getNombreCalle());
                segmentos.add(segmento);
                
                tiempoTotal += segmento.getTiempoEstimadoMinutos();
                distanciaAcumulada += arista.getDistanciaKm();
            }
        }
        
        dto.setSegmentos(segmentos);
        dto.setTiempoEstimadoMinutos(tiempoTotal);
        dto.setConsiderandoTrafico(considerarTrafico);
        
        // Generar instrucciones de navegación
        dto.setInstrucciones(generarInstrucciones(camino, segmentos));
        
        return dto;
    }
    
    /**
     * Convierte un nodo del grafo a DTO
     */
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
    
    /**
     * Genera instrucciones de navegación paso a paso
     */
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
    
    /**
     * Encuentra el nodo más cercano no visitado
     */
    private Graph encontrarNodoMasCercano(Graph nodoActual, List<Graph> destinos, Set<Graph> visitados) {
        Graph nodoMasCercano = null;
        double distanciaMinima = Double.MAX_VALUE;
        
        for (Graph destino : destinos) {
            if (!visitados.contains(destino)) {
                double distancia = nodoActual.calcularDistancia(destino);
                if (distancia < distanciaMinima) {
                    distanciaMinima = distancia;
                    nodoMasCercano = destino;
                }
            }
        }
        
        return nodoMasCercano;
    }
    
    /**
     * Clase auxiliar para almacenar información de Dijkstra
     */
    private static class DijkstraInfo {
        double distancia;
        Graph predecesor;
        
        DijkstraInfo(double distancia, Graph predecesor) {
            this.distancia = distancia;
            this.predecesor = predecesor;
        }
    }
    
    /**
     * Clase auxiliar para la cola de prioridad
     */
    private static class NodoConDistancia implements Comparable<NodoConDistancia> {
        Graph nodo;
        double distancia;
        
        NodoConDistancia(Graph nodo, double distancia) {
            this.nodo = nodo;
            this.distancia = distancia;
        }
        
        @Override
        public int compareTo(NodoConDistancia otro) {
            return Double.compare(this.distancia, otro.distancia);
        }
    }
}