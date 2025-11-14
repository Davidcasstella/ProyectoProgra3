package com.Zygo.proyecto.service;

import com.Zygo.proyecto.dto.RutaOptimaDTO;
import com.Zygo.proyecto.model.*;
import com.Zygo.proyecto.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar el grafo de rutas y su integración con el sistema de pedidos
 */
@Service
public class GraphManagementService {
    
    private static final Logger log = LoggerFactory.getLogger(GraphManagementService.class);
    
    @Autowired
    private GraphRepository graphRepository;
    
    @Autowired
    private EdgeRepository edgeRepository;
    
    @Autowired
    private PedidoRepository pedidoRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private DijkstraService dijkstraService;
    
    /**
     * Inicializa el grafo con datos de ejemplo para la ciudad
     */
    @Transactional
    public void inicializarGrafoCiudad() {
        log.info("Inicializando grafo de la ciudad con datos de ejemplo");
        
        // Verificar si ya hay datos
        if (graphRepository.count() > 0) {
            log.info("El grafo ya está inicializado");
            return;
        }
        
        // Crear nodos de ejemplo (simulando una ciudad)
        List<Graph> nodos = crearNodosEjemplo();
        graphRepository.saveAll(nodos);
        
        // Crear aristas/conexiones entre nodos
        List<Edge> aristas = crearAristasEjemplo(nodos);
        edgeRepository.saveAll(aristas);
        
        log.info("Grafo inicializado con {} nodos y {} aristas", nodos.size(), aristas.size());
    }
    
    /**
     * Crea nodos de ejemplo para el grafo
     */
    private List<Graph> crearNodosEjemplo() {
        List<Graph> nodos = new ArrayList<>();
        
        // Base de repartidores
        nodos.add(new Graph("Base Central Zygo", 5.5397, -73.3618, Graph.TipoNodo.BASE_REPARTIDORES));
        
        // Restaurantes
        nodos.add(new Graph("Pizza Express", 5.5412, -73.3605, Graph.TipoNodo.RESTAURANTE));
        nodos.add(new Graph("Burger King Centro", 5.5385, -73.3625, Graph.TipoNodo.RESTAURANTE));
        nodos.add(new Graph("Sushi House", 5.5420, -73.3595, Graph.TipoNodo.RESTAURANTE));
        nodos.add(new Graph("Pollo Frito KFC", 5.5378, -73.3640, Graph.TipoNodo.RESTAURANTE));
        
        // Intersecciones principales
        nodos.add(new Graph("Plaza Principal", 5.5400, -73.3610, Graph.TipoNodo.INTERSECCION));
        nodos.add(new Graph("Calle 10 con Carrera 5", 5.5390, -73.3620, Graph.TipoNodo.INTERSECCION));
        nodos.add(new Graph("Calle 15 con Carrera 3", 5.5415, -73.3600, Graph.TipoNodo.INTERSECCION));
        nodos.add(new Graph("Rotonda Norte", 5.5425, -73.3590, Graph.TipoNodo.INTERSECCION));
        nodos.add(new Graph("Semáforo Sur", 5.5375, -73.3635, Graph.TipoNodo.INTERSECCION));
        
        // Zonas residenciales (clientes potenciales)
        nodos.add(new Graph("Residencial Las Palmas", 5.5430, -73.3585, Graph.TipoNodo.CLIENTE));
        nodos.add(new Graph("Conjunto Torres del Sol", 5.5370, -73.3645, Graph.TipoNodo.CLIENTE));
        nodos.add(new Graph("Barrio San José", 5.5405, -73.3615, Graph.TipoNodo.CLIENTE));
        nodos.add(new Graph("Urbanización El Recreo", 5.5395, -73.3630, Graph.TipoNodo.CLIENTE));
        nodos.add(new Graph("Edificio Central Park", 5.5410, -73.3608, Graph.TipoNodo.CLIENTE));
        
        return nodos;
    }
    
    /**
     * Crea aristas de ejemplo conectando los nodos
     */
    private List<Edge> crearAristasEjemplo(List<Graph> nodos) {
        List<Edge> aristas = new ArrayList<>();
        Map<String, Graph> nodosMap = nodos.stream()
                .collect(Collectors.toMap(Graph::getNombre, n -> n));
        
        // Conexiones desde la base central
        aristas.add(crearArista(nodosMap.get("Base Central Zygo"), 
                                nodosMap.get("Plaza Principal"), 0.5, 2, "Av. Principal"));
        aristas.add(crearArista(nodosMap.get("Base Central Zygo"), 
                                nodosMap.get("Calle 10 con Carrera 5"), 0.3, 2, "Calle 10"));
        
        // Conexiones entre intersecciones
        aristas.add(crearArista(nodosMap.get("Plaza Principal"), 
                                nodosMap.get("Calle 10 con Carrera 5"), 0.2, 1, "Calle Central"));
        aristas.add(crearArista(nodosMap.get("Plaza Principal"), 
                                nodosMap.get("Calle 15 con Carrera 3"), 0.3, 2, "Carrera 3"));
        aristas.add(crearArista(nodosMap.get("Calle 15 con Carrera 3"), 
                                nodosMap.get("Rotonda Norte"), 0.4, 3, "Av. Norte"));
        aristas.add(crearArista(nodosMap.get("Calle 10 con Carrera 5"), 
                                nodosMap.get("Semáforo Sur"), 0.5, 3, "Av. Sur"));
        
        // Conexiones a restaurantes
        aristas.add(crearArista(nodosMap.get("Plaza Principal"), 
                                nodosMap.get("Pizza Express"), 0.2, 1, "Calle Comercial"));
        aristas.add(crearArista(nodosMap.get("Calle 10 con Carrera 5"), 
                                nodosMap.get("Burger King Centro"), 0.2, 1, "Calle 11"));
        aristas.add(crearArista(nodosMap.get("Calle 15 con Carrera 3"), 
                                nodosMap.get("Sushi House"), 0.3, 2, "Carrera 2"));
        aristas.add(crearArista(nodosMap.get("Semáforo Sur"), 
                                nodosMap.get("Pollo Frito KFC"), 0.2, 1, "Calle Sur"));
        
        // Conexiones a zonas residenciales
        aristas.add(crearArista(nodosMap.get("Rotonda Norte"), 
                                nodosMap.get("Residencial Las Palmas"), 0.3, 2, "Av. Residencial"));
        aristas.add(crearArista(nodosMap.get("Semáforo Sur"), 
                                nodosMap.get("Conjunto Torres del Sol"), 0.3, 2, "Calle Torres"));
        aristas.add(crearArista(nodosMap.get("Plaza Principal"), 
                                nodosMap.get("Barrio San José"), 0.2, 1, "Calle San José"));
        aristas.add(crearArista(nodosMap.get("Calle 10 con Carrera 5"), 
                                nodosMap.get("Urbanización El Recreo"), 0.3, 2, "Diagonal 5"));
        aristas.add(crearArista(nodosMap.get("Plaza Principal"), 
                                nodosMap.get("Edificio Central Park"), 0.1, 1, "Calle Park"));
        
        // Conexiones adicionales para crear múltiples rutas posibles
        aristas.add(crearArista(nodosMap.get("Pizza Express"), 
                                nodosMap.get("Edificio Central Park"), 0.2, 1, "Pasaje Corto"));
        aristas.add(crearArista(nodosMap.get("Burger King Centro"), 
                                nodosMap.get("Urbanización El Recreo"), 0.3, 2, "Calle Alterna"));
        
        return aristas;
    }
    
    /**
     * Crea una arista con los parámetros dados
     */
    private Edge crearArista(Graph origen, Graph destino, Double distanciaKm, 
                             Integer tiempoMin, String nombreCalle) {
        Edge arista = new Edge(origen, destino, distanciaKm, tiempoMin);
        arista.setNombreCalle(nombreCalle);
        arista.setEsBidireccional(true); // Por defecto, calles bidireccionales
        arista.setTipoCalle(Edge.TipoCalle.CALLE_SECUNDARIA);
        arista.setFactorTrafico(1.0); // Sin tráfico inicialmente
        return arista;
    }
    
    /**
     * Calcula la ruta óptima para un pedido específico
     */
    @Transactional(readOnly = true)
    public RutaOptimaDTO calcularRutaPedido(Long pedidoId) {
        log.info("Calculando ruta para pedido {}", pedidoId);
        
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        
        // Encontrar nodos más cercanos al origen y destino
        Graph nodoOrigen = encontrarNodoMasCercano(pedido.getDireccionOrigen());
        Graph nodoDestino = encontrarNodoMasCercano(pedido.getDireccionDestino());
        
        if (nodoOrigen == null || nodoDestino == null) {
            throw new RuntimeException("No se pudieron encontrar nodos para las direcciones del pedido");
        }
        
        // Calcular ruta óptima
        RutaOptimaDTO ruta = dijkstraService.encontrarRutaOptima(
                nodoOrigen.getId(), nodoDestino.getId(), true);
        
        // Actualizar información del pedido con la ruta calculada
        actualizarPedidoConRuta(pedido, ruta);
        
        return ruta;
    }
    
    /**
     * Calcula las rutas óptimas para todos los pedidos de un repartidor
     */
    @Transactional(readOnly = true)
    public List<RutaOptimaDTO> calcularRutasRepartidor(Long repartidorId) {
        log.info("Calculando rutas para repartidor {}", repartidorId);
        
        Usuario repartidor = usuarioRepository.findById(repartidorId)
                .orElseThrow(() -> new RuntimeException("Repartidor no encontrado"));
        
        // Obtener pedidos asignados al repartidor
        List<Pedido> pedidos = pedidoRepository.findByRepartidorId(repartidorId).stream()
                .filter(p -> p.getEstado() == Pedido.EstadoPedido.ASIGNADO || 
                           p.getEstado() == Pedido.EstadoPedido.EN_CAMINO)
                .collect(Collectors.toList());
        
        if (pedidos.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Encontrar nodo base (ubicación actual del repartidor o base central)
        Graph nodoBase = graphRepository.findByTipo(Graph.TipoNodo.BASE_REPARTIDORES).get(0);
        
        // Obtener nodos destino de los pedidos
        List<Long> nodosDestino = pedidos.stream()
                .map(p -> encontrarNodoMasCercano(p.getDireccionDestino()).getId())
                .collect(Collectors.toList());
        
        // Calcular rutas múltiples optimizadas
        return dijkstraService.encontrarRutasMultiples(nodoBase.getId(), nodosDestino);
    }
    
    /**
     * Encuentra el nodo más cercano a una dirección dada
     */
    private Graph encontrarNodoMasCercano(String direccion) {
        // Por ahora, retornamos un nodo aleatorio
        // En producción, esto debería usar geocoding para convertir direcciones a coordenadas
        List<Graph> nodos = graphRepository.findAll();
        if (!nodos.isEmpty()) {
            // Simular búsqueda basada en la dirección
            return nodos.get(Math.abs(direccion.hashCode()) % nodos.size());
        }
        return null;
    }
    
    /**
     * Actualiza un pedido con la información de la ruta calculada
     */
    private void actualizarPedidoConRuta(Pedido pedido, RutaOptimaDTO ruta) {
        // Actualizar distancia si es diferente
        if (ruta.getDistanciaTotalKm() != null && 
            !ruta.getDistanciaTotalKm().equals(pedido.getDistanciaKm())) {
            pedido.setDistanciaKm(ruta.getDistanciaTotalKm());
            
            // Recalcular costo
            Double nuevoCosto = 5000.0 + (ruta.getDistanciaTotalKm() * 2000.0);
            pedido.setCosto(nuevoCosto);
            
            pedidoRepository.save(pedido);
            log.info("Pedido {} actualizado con nueva distancia: {} km", 
                     pedido.getId(), ruta.getDistanciaTotalKm());
        }
    }
    
    /**
     * Actualiza el factor de tráfico de una arista
     */
    @Transactional
    public void actualizarFactorTrafico(Long aristaId, Double factorTrafico) {
        log.info("Actualizando factor de tráfico de arista {} a {}", aristaId, factorTrafico);
        
        Edge arista = edgeRepository.findById(aristaId)
                .orElseThrow(() -> new RuntimeException("Arista no encontrada"));
        
        if (factorTrafico < 0.5 || factorTrafico > 5.0) {
            throw new IllegalArgumentException("Factor de tráfico debe estar entre 0.5 y 5.0");
        }
        
        arista.setFactorTrafico(factorTrafico);
        edgeRepository.save(arista);
    }
    
    /**
     * Obtiene información del estado del tráfico
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerEstadoTrafico() {
        Map<String, Object> estadoTrafico = new HashMap<>();
        
        // Obtener promedio de tráfico
        Double promedioTrafico = edgeRepository.obtenerPromedioTrafico();
        estadoTrafico.put("promedioGeneral", promedioTrafico != null ? promedioTrafico : 1.0);
        
        // Obtener aristas congestionadas (factor > 1.5)
        List<Edge> aristasCongestionadas = edgeRepository.findAristasConTrafico(1.5);
        estadoTrafico.put("rutasCongestionadas", aristasCongestionadas.size());
        
        // Detalles de congestión
        List<Map<String, Object>> detallesCongesion = aristasCongestionadas.stream()
                .map(a -> {
                    Map<String, Object> detalle = new HashMap<>();
                    detalle.put("id", a.getId());
                    detalle.put("calle", a.getNombreCalle());
                    detalle.put("factorTrafico", a.getFactorTrafico());
                    detalle.put("origen", a.getNodoOrigen().getNombre());
                    detalle.put("destino", a.getNodoDestino().getNombre());
                    return detalle;
                })
                .collect(Collectors.toList());
        
        estadoTrafico.put("detallesCongestion", detallesCongesion);
        
        return estadoTrafico;
    }
    
    /**
     * Simula actualización de tráfico en tiempo real
     */
    @Transactional
    public void simularTraficoTiempoReal() {
        log.info("Simulando actualización de tráfico en tiempo real");
        
        List<Edge> todasLasAristas = edgeRepository.findAll();
        Random random = new Random();
        
        for (Edge arista : todasLasAristas) {
            // Simular cambios de tráfico aleatorios
            double nuevoFactor = 0.8 + (random.nextDouble() * 1.7); // Entre 0.8 y 2.5
            
            // Las avenidas principales tienen más probabilidad de tráfico
            if (arista.getTipoCalle() == Edge.TipoCalle.AVENIDA_PRINCIPAL) {
                nuevoFactor *= 1.3;
            }
            
            // Limitar el factor entre 0.5 y 3.0
            nuevoFactor = Math.max(0.5, Math.min(3.0, nuevoFactor));
            
            arista.setFactorTrafico(nuevoFactor);
        }
        
        edgeRepository.saveAll(todasLasAristas);
        log.info("Tráfico actualizado para {} aristas", todasLasAristas.size());
    }
}