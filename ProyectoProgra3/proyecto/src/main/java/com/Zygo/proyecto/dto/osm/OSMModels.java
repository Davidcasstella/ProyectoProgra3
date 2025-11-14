package com.Zygo.proyecto.dto.osm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Modelos para parsear la respuesta de Overpass API
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class OSMModels {
    
    /**
     * Respuesta principal de Overpass API
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OSMResponse {
        private String version;
        private String generator;
        private List<OSMElement> elements;
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public String getGenerator() { return generator; }
        public void setGenerator(String generator) { this.generator = generator; }
        
        public List<OSMElement> getElements() { return elements; }
        public void setElements(List<OSMElement> elements) { this.elements = elements; }
    }
    
    /**
     * Elemento de OSM (puede ser nodo o vía)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OSMElement {
        private String type;
        private Long id;
        private Double lat;
        private Double lon;
        private Map<String, String> tags;
        private List<Long> nodes;  // Para ways
        private List<OSMNode> geometry;  // Para ways con geometría
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public Double getLat() { return lat; }
        public void setLat(Double lat) { this.lat = lat; }
        
        public Double getLon() { return lon; }
        public void setLon(Double lon) { this.lon = lon; }
        
        public Map<String, String> getTags() { return tags; }
        public void setTags(Map<String, String> tags) { this.tags = tags; }
        
        public List<Long> getNodes() { return nodes; }
        public void setNodes(List<Long> nodes) { this.nodes = nodes; }
        
        public List<OSMNode> getGeometry() { return geometry; }
        public void setGeometry(List<OSMNode> geometry) { this.geometry = geometry; }
        
        // Helpers
        public boolean isNode() { return "node".equals(type); }
        public boolean isWay() { return "way".equals(type); }
        
        public String getTag(String key) {
            return tags != null ? tags.get(key) : null;
        }
        
        public String getName() {
            return getTag("name");
        }
        
        public String getHighwayType() {
            return getTag("highway");
        }
        
        public boolean isOneWay() {
            String oneway = getTag("oneway");
            return "yes".equals(oneway) || "true".equals(oneway) || "1".equals(oneway);
        }
    }
    
    /**
     * Nodo individual con coordenadas
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OSMNode {
        private Long id;
        private Double lat;
        private Double lon;
        
        public OSMNode() {}
        
        public OSMNode(Long id, Double lat, Double lon) {
            this.id = id;
            this.lat = lat;
            this.lon = lon;
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public Double getLat() { return lat; }
        public void setLat(Double lat) { this.lat = lat; }
        
        public Double getLon() { return lon; }
        public void setLon(Double lon) { this.lon = lon; }
    }
    
    /**
     * Estadísticas de importación
     */
    public static class ImportStats {
        private int totalElementos;
        private int nodosCreados;
        private int aristasCreadas;
        private int elementosDescartados;
        private long tiempoMs;
        private String mensaje;
        
        public ImportStats() {}
        
        public int getTotalElementos() { return totalElementos; }
        public void setTotalElementos(int totalElementos) { this.totalElementos = totalElementos; }
        
        public int getNodosCreados() { return nodosCreados; }
        public void setNodosCreados(int nodosCreados) { this.nodosCreados = nodosCreados; }
        
        public int getAristasCreadas() { return aristasCreadas; }
        public void setAristasCreadas(int aristasCreadas) { this.aristasCreadas = aristasCreadas; }
        
        public int getElementosDescartados() { return elementosDescartados; }
        public void setElementosDescartados(int elementosDescartados) { 
            this.elementosDescartados = elementosDescartados; 
        }
        
        public long getTiempoMs() { return tiempoMs; }
        public void setTiempoMs(long tiempoMs) { this.tiempoMs = tiempoMs; }
        
        public String getMensaje() { return mensaje; }
        public void setMensaje(String mensaje) { this.mensaje = mensaje; }
        
        @Override
        public String toString() {
            return String.format(
                "Importación completada: %d nodos, %d aristas creadas en %d ms. " +
                "Descartados: %d (total procesados: %d)",
                nodosCreados, aristasCreadas, tiempoMs, elementosDescartados, totalElementos
            );
        }
    }
}