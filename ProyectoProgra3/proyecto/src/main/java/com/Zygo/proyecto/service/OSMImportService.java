package com.Zygo.proyecto.service;

import com.Zygo.proyecto.dto.osm.OSMModels.*;
import com.Zygo.proyecto.model.Edge;
import com.Zygo.proyecto.model.Graph;
import com.Zygo.proyecto.repository.EdgeRepository;
import com.Zygo.proyecto.repository.GraphRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para importar el mapa completo de Sogamoso desde OpenStreetMap
 */
@Service
public class OSMImportService {
    
    private static final Logger log = LoggerFactory.getLogger(OSMImportService.class);
    
    // Coordenadas del área de Sogamoso, Boyacá
    private static final double SOGAMOSO_MIN_LAT = 5.695;  // Sur
    private static final double SOGAMOSO_MAX_LAT = 5.735;  // Norte
    private static final double SOGAMOSO_MIN_LON = -72.950; // Oeste
    private static final double SOGAMOSO_MAX_LON = -72.915; // Este
    
    // URL de Overpass API
    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
    
    @Autowired
    private GraphRepository graphRepository;
    
    @Autowired
    private EdgeRepository edgeRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
   @Transactional
public ImportStats importarMapaSogamoso(boolean incluirCallesResidenciales) {
    log.info("═══════════════════════════════════════════════════════");
    log.info("🗺️  INICIANDO IMPORTACIÓN DE SOGAMOSO DESDE OSM");
    log.info("═══════════════════════════════════════════════════════");
    
    long inicioMs = System.currentTimeMillis();
    ImportStats stats = new ImportStats();
    
    try {
        // 1. LIMPIAR AUTOMÁTICAMENTE si ya hay datos
        long nodosExistentes = graphRepository.count();
        if (nodosExistentes > 0) {
            log.warn("⚠️  Ya existen {} nodos en la BD", nodosExistentes);
            log.warn("⚠️  Limpiando datos anteriores automáticamente...");
            limpiarGrafo(); // ← AGREGAR: Limpieza automática
            log.info("✓ BD limpia, iniciando importación fresca");
        }
        
        // 2. Descargar datos de OSM
        log.info("📥 Descargando datos de OpenStreetMap...");
        OSMResponse osmData = descargarDatosOSM(incluirCallesResidenciales);
        stats.setTotalElementos(osmData.getElements().size());
        log.info("✓ Descargados {} elementos de OSM", stats.getTotalElementos());
        
        // 3. Procesar nodos (intersecciones)
        log.info("🔄 Procesando intersecciones...");
        Map<Long, Graph> nodosMap = procesarNodos(osmData);
        stats.setNodosCreados(nodosMap.size()); // ← AHORA SÍ CONTARÁ CORRECTAMENTE
        log.info("✓ {} intersecciones creadas", stats.getNodosCreados());
        
        // 4. Procesar calles (aristas)
        log.info("🔄 Procesando calles...");
        int aristasCreadas = procesarCalles(osmData, nodosMap);
        stats.setAristasCreadas(aristasCreadas);
        log.info("✓ {} conexiones viales creadas", aristasCreadas);
        
        // 5. Estadísticas finales
        stats.setElementosDescartados(
            stats.getTotalElementos() - stats.getNodosCreados() - stats.getAristasCreadas()
        );
        stats.setTiempoMs(System.currentTimeMillis() - inicioMs);
        stats.setMensaje("Importación exitosa");
        
        log.info("═══════════════════════════════════════════════════════");
        log.info("✅ IMPORTACIÓN COMPLETADA EXITOSAMENTE");
        log.info("   📍 Nodos creados: {}", stats.getNodosCreados());
        log.info("   🛣️  Aristas creadas: {}", stats.getAristasCreadas());
        log.info("   ⏱️  Tiempo: {} segundos", stats.getTiempoMs() / 1000.0);
        log.info("═══════════════════════════════════════════════════════");
        
        return stats;
        
    } catch (Exception e) {
        log.error("❌ Error durante la importación: {}", e.getMessage(), e);
        stats.setMensaje("Error: " + e.getMessage());
        stats.setTiempoMs(System.currentTimeMillis() - inicioMs);
        throw new RuntimeException("Error importando mapa de Sogamoso", e);
    }
}
    /**
     * Descarga datos de OSM usando Overpass API
     */
    private OSMResponse descargarDatosOSM(boolean incluirResidenciales) throws Exception {
        String query = construirQueryOverpass(incluirResidenciales);
        
        log.debug("Query Overpass: {}", query);
        
        try {
            String response = restTemplate.postForObject(
                OVERPASS_URL, 
                query, 
                String.class
            );
            
            if (response == null || response.isEmpty()) {
                throw new RuntimeException("Respuesta vacía de Overpass API");
            }
            
            return objectMapper.readValue(response, OSMResponse.class);
            
        } catch (Exception e) {
            log.error("Error descargando datos de OSM: {}", e.getMessage());
            throw new RuntimeException("No se pudo conectar con Overpass API. " +
                "Verifica tu conexión a internet.", e);
        }
    }
    
    /**
     * Construye la query de Overpass para Sogamoso
     */
    private String construirQueryOverpass(boolean incluirResidenciales) {
    String bbox = String.format(Locale.US, "%.6f,%.6f,%.6f,%.6f", 
        SOGAMOSO_MIN_LAT, SOGAMOSO_MIN_LON, 
        SOGAMOSO_MAX_LAT, SOGAMOSO_MAX_LON);
        
        StringBuilder query = new StringBuilder();
        query.append("[out:json][timeout:90];");
        query.append("(");
        
        // Calles principales (siempre incluidas)
        query.append("way[\"highway\"=\"primary\"](").append(bbox).append(");");
        query.append("way[\"highway\"=\"secondary\"](").append(bbox).append(");");
        query.append("way[\"highway\"=\"tertiary\"](").append(bbox).append(");");
        query.append("way[\"highway\"=\"trunk\"](").append(bbox).append(");");
        
        // Calles residenciales (opcional, pero recomendado)
        if (incluirResidenciales) {
            query.append("way[\"highway\"=\"residential\"](").append(bbox).append(");");
            query.append("way[\"highway\"=\"living_street\"](").append(bbox).append(");");
        }
        
        // Calles adicionales útiles
        query.append("way[\"highway\"=\"unclassified\"](").append(bbox).append(");");
        query.append("way[\"highway\"=\"service\"](").append(bbox).append(");");
        
        query.append(");");
        query.append("out geom;");
        
        return query.toString();
    }
    
 /**
 * Procesa nodos de OSM y los convierte a entidades Graph
 */
private Map<Long, Graph> procesarNodos(OSMResponse osmData) {
    Map<String, Graph> nodosPorCoordenadas = new HashMap<>();
    
    // 1. Recolectar todos los nodos únicos de las vías
    for (OSMElement element : osmData.getElements()) {
        if (element.isWay() && element.getGeometry() != null) {
            for (OSMNode osmNode : element.getGeometry()) {
                if (osmNode.getLat() == null || osmNode.getLon() == null) {
                    continue;
                }
                
                String coordKey = String.format(Locale.US, "%.6f,%.6f", 
                    osmNode.getLat(), osmNode.getLon());
                
                if (!nodosPorCoordenadas.containsKey(coordKey)) {
                    Graph nodo = convertirOSMNodeAGraph(osmNode, element);
                    nodosPorCoordenadas.put(coordKey, nodo);
                }
            }
        }
    }
    
    log.info("   Nodos únicos recolectados: {}", nodosPorCoordenadas.size());
    
    // 2. Guardar todos los nodos en BD
    List<Graph> nodosGuardados = graphRepository.saveAll(nodosPorCoordenadas.values());
    log.info("   Nodos guardados en BD: {}", nodosGuardados.size());
    
    // 3. Crear mapa de ID a Graph guardado (ESTO ES LO QUE FALTABA)
    Map<Long, Graph> mapaRetorno = new HashMap<>();
    for (Graph nodo : nodosGuardados) {
        mapaRetorno.put(nodo.getId(), nodo);
    }
    
    log.info("   ✓ {} nodos listos para procesar calles", mapaRetorno.size());
    
    return mapaRetorno; // ← CAMBIO CRÍTICO: antes retornaba HashMap vacío
}

    
    /**
 * Procesa calles de OSM y las convierte a aristas Edge
 */
private int procesarCalles(OSMResponse osmData, Map<Long, Graph> nodosMap) {
    // Crear mapa de coordenadas a nodos guardados
    Map<String, Graph> nodosPorCoordenadas = new HashMap<>();
    for (Graph nodo : graphRepository.findAll()) {
        String coordKey = String.format(Locale.US, "%.6f,%.6f", 
            nodo.getLatitud(), nodo.getLongitud());
        nodosPorCoordenadas.put(coordKey, nodo);
    }
    
    List<Edge> aristas = new ArrayList<>();
    int callesProcesadas = 0;
    
    for (OSMElement way : osmData.getElements()) {
        if (!way.isWay() || way.getGeometry() == null || way.getGeometry().size() < 2) {
            continue;
        }
        
        callesProcesadas++;
        String nombreCalle = way.getName() != null ? way.getName() : "Calle sin nombre";
        Edge.TipoCalle tipoCalle = determinarTipoCalle(way.getHighwayType());
        boolean esBidireccional = !way.isOneWay();
        
        // Crear aristas entre nodos consecutivos
        List<OSMNode> geometria = way.getGeometry();
        for (int i = 0; i < geometria.size() - 1; i++) {
            OSMNode nodoA = geometria.get(i);
            OSMNode nodoB = geometria.get(i + 1);
            
            if (nodoA.getLat() == null || nodoA.getLon() == null ||
                nodoB.getLat() == null || nodoB.getLon() == null) {
                continue;
            }
            
            // Buscar nodos por coordenadas
            String coordKeyA = String.format(Locale.US, "%.6f,%.6f", 
                nodoA.getLat(), nodoA.getLon());
            String coordKeyB = String.format(Locale.US, "%.6f,%.6f", 
                nodoB.getLat(), nodoB.getLon());
            
            Graph graphA = nodosPorCoordenadas.get(coordKeyA);
            Graph graphB = nodosPorCoordenadas.get(coordKeyB);
            
            if (graphA != null && graphB != null && !graphA.getId().equals(graphB.getId())) {
                // Crear arista
                Edge arista = crearArista(graphA, graphB, nombreCalle, 
                    tipoCalle, esBidireccional);
                aristas.add(arista);
                
                // Si es bidireccional, crear arista inversa
                if (esBidireccional) {
                    Edge aristaInversa = crearArista(graphB, graphA, nombreCalle, 
                        tipoCalle, esBidireccional);
                    aristas.add(aristaInversa);
                }
            }
        }
    }
    
    // Guardar aristas en BD
    if (!aristas.isEmpty()) {
        edgeRepository.saveAll(aristas);
    }
    
    log.info("   Procesadas {} calles de OSM", callesProcesadas);
    return aristas.size();
}
    
    /**
 * Convierte un nodo de OSM a entidad Graph
 */
private Graph convertirOSMNodeAGraph(OSMNode osmNode, OSMElement calle) {
    Graph nodo = new Graph();
    
    // Generar nombre único basado en coordenadas (no en ID que puede ser null)
    String nombreUnico = String.format(Locale.US, "Nodo_%.6f_%.6f", 
        osmNode.getLat(), osmNode.getLon());
    nodo.setNombre(nombreUnico);
    
    nodo.setLatitud(osmNode.getLat());
    nodo.setLongitud(osmNode.getLon());
    nodo.setTipo(Graph.TipoNodo.INTERSECCION);
    nodo.setActivo(true);
    
    // Guardar info adicional
    String direccion = calle.getName() != null ? 
        "Cerca de " + calle.getName() : 
        String.format(Locale.US, "%.6f, %.6f", osmNode.getLat(), osmNode.getLon());
    nodo.setDireccionCompleta(direccion);
    
    // Guardar coordenadas en descripción para tracking
    nodo.setDescripcion(String.format(Locale.US, "OSM:%.6f,%.6f", 
        osmNode.getLat(), osmNode.getLon()));
    
    return nodo;
}
    
    /**
     * Crea una arista entre dos nodos
     */
    private Edge crearArista(Graph origen, Graph destino, String nombreCalle,
                            Edge.TipoCalle tipoCalle, boolean bidireccional) {
        Edge arista = new Edge();
        arista.setNodoOrigen(origen);
        arista.setNodoDestino(destino);
        arista.setNombreCalle(nombreCalle);
        arista.setTipoCalle(tipoCalle);
        arista.setEsBidireccional(bidireccional);
        arista.setActivo(true);
        
        // Calcular distancia real usando fórmula de Haversine
        double distanciaKm = calcularDistanciaHaversine(
            origen.getLatitud(), origen.getLongitud(),
            destino.getLatitud(), destino.getLongitud()
        );
        arista.setDistanciaKm(distanciaKm);
        
        // Estimar tiempo basado en tipo de calle
        int tiempoMinutos = estimarTiempoViaje(distanciaKm, tipoCalle);
        arista.setTiempoEstimadoMinutos(tiempoMinutos);
        
        // Factor de tráfico inicial
        arista.setFactorTrafico(1.0);
        
        return arista;
    }
    
    /**
     * Determina el tipo de calle según clasificación OSM
     */
    private Edge.TipoCalle determinarTipoCalle(String highwayType) {
        if (highwayType == null) return Edge.TipoCalle.CALLE_SECUNDARIA;
        
        switch (highwayType.toLowerCase()) {
            case "motorway":
            case "trunk":
            case "primary":
                return Edge.TipoCalle.AVENIDA_PRINCIPAL;
            case "secondary":
            case "tertiary":
                return Edge.TipoCalle.CALLE_SECUNDARIA;
            case "residential":
            case "living_street":
            case "unclassified":
                return Edge.TipoCalle.CALLE_SECUNDARIA;
            case "service":
                return Edge.TipoCalle.CALLEJON;
            case "pedestrian":
            case "footway":
            case "path":
                return Edge.TipoCalle.PEATONAL;
            case "cycleway":
                return Edge.TipoCalle.CICLOVIA;
            default:
                return Edge.TipoCalle.CALLE_SECUNDARIA;
        }
    }
    
    /**
     * Calcula distancia real entre dos puntos usando fórmula de Haversine
     */
    private double calcularDistanciaHaversine(double lat1, double lon1, 
                                              double lat2, double lon2) {
        final double R = 6371; // Radio de la Tierra en km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * Estima tiempo de viaje según distancia y tipo de calle
     */
    private int estimarTiempoViaje(double distanciaKm, Edge.TipoCalle tipoCalle) {
        // Velocidades promedio en km/h
        double velocidadKmH;
        switch (tipoCalle) {
            case AVENIDA_PRINCIPAL:
                velocidadKmH = 40;
                break;
            case CALLE_SECUNDARIA:
                velocidadKmH = 30;
                break;
            case CALLEJON:
                velocidadKmH = 20;
                break;
            case PEATONAL:
            case CICLOVIA:
                velocidadKmH = 15;
                break;
            default:
                velocidadKmH = 25;
        }
        
        double tiempoHoras = distanciaKm / velocidadKmH;
        int tiempoMinutos = (int) Math.ceil(tiempoHoras * 60);
        
        return Math.max(1, tiempoMinutos); // Mínimo 1 minuto
    }
    
    /**
 * Extrae el OSM ID original del nodo (ahora usa coordenadas)
 */
private Long extraerOSMIdDeNodo(Graph nodo) {
    // Ya no usamos OSM IDs, sino coordenadas como identificador
    // Este método ya no es necesario, pero lo dejamos para compatibilidad
    return nodo.getId(); // Retorna el ID de la BD
}
    
   /**
 * Limpia todos los datos del grafo (usar con precaución)
 */
@Transactional
public void limpiarGrafo() {
    log.warn("⚠️  Eliminando todos los datos del grafo...");
    
    // IMPORTANTE: Eliminar en orden correcto y con flush
    long aristasEliminadas = edgeRepository.count();
    edgeRepository.deleteAll();
    edgeRepository.flush(); // ← AGREGAR: Forzar eliminación inmediata
    
    long nodosEliminados = graphRepository.count();
    graphRepository.deleteAll();
    graphRepository.flush(); // ← AGREGAR: Forzar eliminación inmediata
    
    log.info("✓ {} aristas eliminadas", aristasEliminadas);
    log.info("✓ {} nodos eliminados", nodosEliminados);
    log.info("✓ Grafo limpiado completamente");
}
}