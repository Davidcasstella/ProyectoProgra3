package com.Zygo.proyecto.service;

import com.Zygo.proyecto.dto.RutaOptimaDTO;
import com.Zygo.proyecto.model.Graph;
import com.Zygo.proyecto.repository.GraphRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para buscar lugares y calcular rutas por nombre
 */
@Service
public class LugarService {
    
    private static final Logger log = LoggerFactory.getLogger(LugarService.class);
    
    // Radio de búsqueda en grados (aprox. 1 km)
    private static final double RADIO_BUSQUEDA_DEFAULT = 0.01;
    
    @Autowired
    private GraphRepository graphRepository;
    
    @Autowired
    private DijkstraService dijkstraService;
    
    /**
     * Busca lugares por nombre (búsqueda flexible)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> buscarLugaresPorNombre(String query) {
        log.info("Buscando lugares con query: '{}'", query);
        
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String queryLower = query.toLowerCase().trim();
        
        // Buscar en todos los nodos
        List<Graph> todosLosNodos = graphRepository.findAll();
        
        return todosLosNodos.stream()
                .filter(nodo -> {
                    String nombre = nodo.getNombre().toLowerCase();
                    String direccion = nodo.getDireccionCompleta() != null 
                        ? nodo.getDireccionCompleta().toLowerCase() 
                        : "";
                    
                    // Búsqueda flexible: contiene, palabras, etc.
                    return nombre.contains(queryLower) || 
                           direccion.contains(queryLower) ||
                           calcularSimilitud(nombre, queryLower) > 0.6;
                })
                .sorted((a, b) -> {
                    // Ordenar por relevancia
                    int scoreA = calcularScore(a, queryLower);
                    int scoreB = calcularScore(b, queryLower);
                    return Integer.compare(scoreB, scoreA);
                })
                .limit(10) // Top 10 resultados
                .map(this::convertirNodoAMapa)
                .collect(Collectors.toList());
    }
    
    /**
     * Busca el lugar más cercano a una búsqueda
     */
    @Transactional(readOnly = true)
    public Graph buscarLugarMasCercano(String query) {
        log.info("Buscando mejor coincidencia para: '{}'", query);
        
        List<Map<String, Object>> resultados = buscarLugaresPorNombre(query);
        
        if (resultados.isEmpty()) {
            log.warn("No se encontraron resultados para: '{}'", query);
            return null;
        }
        
        // Retornar el primer resultado (mejor coincidencia)
        Long nodoId = (Long) resultados.get(0).get("id");
        return graphRepository.findById(nodoId).orElse(null);
    }
    
    /**
     * Encuentra el nodo más cercano a unas coordenadas
     */
    @Transactional(readOnly = true)
    public Graph encontrarNodoMasCercano(Double lat, Double lon) {
        log.info("Buscando nodo más cercano a {}, {}", lat, lon);
        
        List<Graph> nodosCercanos = graphRepository.findNodosCercanos(
            lat, lon, RADIO_BUSQUEDA_DEFAULT
        );
        
        if (nodosCercanos.isEmpty()) {
            // Ampliar búsqueda
            nodosCercanos = graphRepository.findNodosCercanos(lat, lon, 0.05);
        }
        
        if (nodosCercanos.isEmpty()) {
            log.warn("No se encontraron nodos cercanos a {}, {}", lat, lon);
            return null;
        }
        
        // Retornar el más cercano
        return nodosCercanos.stream()
                .min(Comparator.comparingDouble(nodo -> 
                    calcularDistancia(lat, lon, nodo.getLatitud(), nodo.getLongitud())))
                .orElse(null);
    }
    
    /**
     * Calcula ruta entre dos lugares
     */
    @Transactional(readOnly = true)
    public RutaOptimaDTO calcularRutaEntreLugares(Graph origen, Graph destino, 
                                                   boolean considerarTrafico) {
        log.info("Calculando ruta de '{}' a '{}'", origen.getNombre(), destino.getNombre());
        
        return dijkstraService.encontrarRutaOptima(
            origen.getId(), 
            destino.getId(), 
            considerarTrafico
        );
    }
    
    /**
     * Lista lugares disponibles (para autocompletar)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarLugaresDisponibles(String tipoFiltro, int limite) {
        List<Graph> nodos;
        
        if (tipoFiltro != null && !tipoFiltro.isEmpty()) {
            try {
                Graph.TipoNodo tipo = Graph.TipoNodo.valueOf(tipoFiltro.toUpperCase());
                nodos = graphRepository.findByTipo(tipo);
            } catch (IllegalArgumentException e) {
                log.warn("Tipo de nodo inválido: {}", tipoFiltro);
                nodos = graphRepository.findAll();
            }
        } else {
            nodos = graphRepository.findAll();
        }
        
        return nodos.stream()
                .limit(limite)
                .map(this::convertirNodoAMapa)
                .collect(Collectors.toList());
    }
    
    /**
     * Busca lugares cercanos a unas coordenadas
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> buscarLugaresCercanos(Double lat, Double lon, Double radioKm) {
        // Convertir km a grados (aproximación)
        double radioDegrees = radioKm / 111.0; // 1 grado ≈ 111 km
        
        List<Graph> nodosCercanos = graphRepository.findNodosCercanos(lat, lon, radioDegrees);
        
        return nodosCercanos.stream()
                .map(nodo -> {
                    Map<String, Object> mapa = convertirNodoAMapa(nodo);
                    double distancia = calcularDistancia(lat, lon, 
                        nodo.getLatitud(), nodo.getLongitud());
                    mapa.put("distanciaKm", Math.round(distancia * 100.0) / 100.0);
                    return mapa;
                })
                .sorted(Comparator.comparingDouble(m -> (Double) m.get("distanciaKm")))
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene sugerencias para una búsqueda sin resultados
     */
    @Transactional(readOnly = true)
    public List<String> obtenerSugerencias(String queryFallido) {
        // Retornar lugares populares como sugerencias
        return graphRepository.findAll().stream()
                .filter(n -> n.getTipo() == Graph.TipoNodo.RESTAURANTE || 
                           n.getTipo() == Graph.TipoNodo.INTERSECCION)
                .limit(5)
                .map(Graph::getNombre)
                .collect(Collectors.toList());
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    /**
     * Convierte un nodo a mapa para JSON
     */
    private Map<String, Object> convertirNodoAMapa(Graph nodo) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("id", nodo.getId());
        mapa.put("nombre", nodo.getNombre());
        mapa.put("tipo", nodo.getTipo().toString());
        mapa.put("direccion", nodo.getDireccionCompleta());
        mapa.put("coordenadas", Map.of(
            "lat", nodo.getLatitud(),
            "lon", nodo.getLongitud()
        ));
        return mapa;
    }
    
    /**
     * Calcula score de relevancia para ordenar resultados
     */
    private int calcularScore(Graph nodo, String query) {
        String nombreLower = nodo.getNombre().toLowerCase();
        int score = 0;
        
        // Coincidencia exacta: +100
        if (nombreLower.equals(query)) {
            score += 100;
        }
        
        // Empieza con: +50
        if (nombreLower.startsWith(query)) {
            score += 50;
        }
        
        // Contiene: +25
        if (nombreLower.contains(query)) {
            score += 25;
        }
        
        // Bonus por tipo de nodo
        if (nodo.getTipo() == Graph.TipoNodo.RESTAURANTE || 
            nodo.getTipo() == Graph.TipoNodo.CLIENTE) {
            score += 10;
        }
        
        return score;
    }
    
    /**
     * Calcula similitud entre dos strings (Levenshtein simplificado)
     */
    private double calcularSimilitud(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        
        int maxLen = Math.max(s1.length(), s2.length());
        int distance = levenshteinDistance(s1, s2);
        
        return 1.0 - ((double) distance / maxLen);
    }
    
    /**
     * Distancia de Levenshtein
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    /**
     * Calcula distancia entre dos coordenadas (Haversine)
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