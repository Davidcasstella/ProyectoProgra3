package com.Zygo.proyecto.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO que representa una ruta óptima calculada por el algoritmo de Dijkstra
 */
public class RutaOptimaDTO {
    
    private List<NodoDTO> nodos;
    private List<SegmentoRuta> segmentos;
    private Double distanciaTotalKm;
    private Integer tiempoEstimadoMinutos;
    private Double costoEstimado;
    private Boolean considerandoTrafico;
    private List<String> instrucciones;
    
    // Constructores
    public RutaOptimaDTO() {
        this.nodos = new ArrayList<>();
        this.segmentos = new ArrayList<>();
        this.instrucciones = new ArrayList<>();
    }
    
    public RutaOptimaDTO(List<NodoDTO> nodos, List<SegmentoRuta> segmentos, 
                         Double distanciaTotalKm, Integer tiempoEstimadoMinutos) {
        this.nodos = nodos;
        this.segmentos = segmentos;
        this.distanciaTotalKm = distanciaTotalKm;
        this.tiempoEstimadoMinutos = tiempoEstimadoMinutos;
        this.instrucciones = new ArrayList<>();
        calcularCostoEstimado();
    }
    
    /**
     * Calcula el costo estimado basado en la distancia
     */
    private void calcularCostoEstimado() {
        if (distanciaTotalKm != null) {
            this.costoEstimado = 5000.0 + (distanciaTotalKm * 2000.0);
        }
    }
    
    // Getters y Setters
    public List<NodoDTO> getNodos() {
        return nodos;
    }
    
    public void setNodos(List<NodoDTO> nodos) {
        this.nodos = nodos;
    }
    
    public List<SegmentoRuta> getSegmentos() {
        return segmentos;
    }
    
    public void setSegmentos(List<SegmentoRuta> segmentos) {
        this.segmentos = segmentos;
    }
    
    public Double getDistanciaTotalKm() {
        return distanciaTotalKm;
    }
    
    public void setDistanciaTotalKm(Double distanciaTotalKm) {
        this.distanciaTotalKm = distanciaTotalKm;
        calcularCostoEstimado();
    }
    
    public Integer getTiempoEstimadoMinutos() {
        return tiempoEstimadoMinutos;
    }
    
    public void setTiempoEstimadoMinutos(Integer tiempoEstimadoMinutos) {
        this.tiempoEstimadoMinutos = tiempoEstimadoMinutos;
    }
    
    public Double getCostoEstimado() {
        return costoEstimado;
    }
    
    public void setCostoEstimado(Double costoEstimado) {
        this.costoEstimado = costoEstimado;
    }
    
    public Boolean getConsiderandoTrafico() {
        return considerandoTrafico;
    }
    
    public void setConsiderandoTrafico(Boolean considerandoTrafico) {
        this.considerandoTrafico = considerandoTrafico;
    }
    
    public List<String> getInstrucciones() {
        return instrucciones;
    }
    
    public void setInstrucciones(List<String> instrucciones) {
        this.instrucciones = instrucciones;
    }
    
    /**
     * Clase interna que representa un nodo en la ruta
     */
    public static class NodoDTO {
        private Long id;
        private String nombre;
        private Double latitud;
        private Double longitud;
        private String tipo;
        private String direccion;
        
        // Constructores
        public NodoDTO() {}
        
        public NodoDTO(Long id, String nombre, Double latitud, Double longitud, String tipo) {
            this.id = id;
            this.nombre = nombre;
            this.latitud = latitud;
            this.longitud = longitud;
            this.tipo = tipo;
        }
        
        // Getters y Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getNombre() {
            return nombre;
        }
        
        public void setNombre(String nombre) {
            this.nombre = nombre;
        }
        
        public Double getLatitud() {
            return latitud;
        }
        
        public void setLatitud(Double latitud) {
            this.latitud = latitud;
        }
        
        public Double getLongitud() {
            return longitud;
        }
        
        public void setLongitud(Double longitud) {
            this.longitud = longitud;
        }
        
        public String getTipo() {
            return tipo;
        }
        
        public void setTipo(String tipo) {
            this.tipo = tipo;
        }
        
        public String getDireccion() {
            return direccion;
        }
        
        public void setDireccion(String direccion) {
            this.direccion = direccion;
        }
    }
    
    /**
     * Clase interna que representa un segmento de la ruta (calle entre dos nodos)
     */
    public static class SegmentoRuta {
        private Long nodoOrigenId;
        private Long nodoDestinoId;
        private Double distanciaKm;
        private Integer tiempoEstimadoMinutos;
        private String nombreCalle;
        private Double factorTrafico;
        
        // Constructores
        public SegmentoRuta() {}
        
        public SegmentoRuta(Long nodoOrigenId, Long nodoDestinoId, 
                           Double distanciaKm, Integer tiempoEstimadoMinutos, String nombreCalle) {
            this.nodoOrigenId = nodoOrigenId;
            this.nodoDestinoId = nodoDestinoId;
            this.distanciaKm = distanciaKm;
            this.tiempoEstimadoMinutos = tiempoEstimadoMinutos;
            this.nombreCalle = nombreCalle;
        }
        
        // Getters y Setters
        public Long getNodoOrigenId() {
            return nodoOrigenId;
        }
        
        public void setNodoOrigenId(Long nodoOrigenId) {
            this.nodoOrigenId = nodoOrigenId;
        }
        
        public Long getNodoDestinoId() {
            return nodoDestinoId;
        }
        
        public void setNodoDestinoId(Long nodoDestinoId) {
            this.nodoDestinoId = nodoDestinoId;
        }
        
        public Double getDistanciaKm() {
            return distanciaKm;
        }
        
        public void setDistanciaKm(Double distanciaKm) {
            this.distanciaKm = distanciaKm;
        }
        
        public Integer getTiempoEstimadoMinutos() {
            return tiempoEstimadoMinutos;
        }
        
        public void setTiempoEstimadoMinutos(Integer tiempoEstimadoMinutos) {
            this.tiempoEstimadoMinutos = tiempoEstimadoMinutos;
        }
        
        public String getNombreCalle() {
            return nombreCalle;
        }
        
        public void setNombreCalle(String nombreCalle) {
            this.nombreCalle = nombreCalle;
        }
        
        public Double getFactorTrafico() {
            return factorTrafico;
        }
        
        public void setFactorTrafico(Double factorTrafico) {
            this.factorTrafico = factorTrafico;
        }
    }
    
    @Override
    public String toString() {
        return "RutaOptimaDTO{" +
                "nodos=" + (nodos != null ? nodos.size() : 0) +
                ", distanciaTotalKm=" + distanciaTotalKm +
                ", tiempoEstimadoMinutos=" + tiempoEstimadoMinutos +
                ", costoEstimado=" + costoEstimado +
                '}';
    }
}