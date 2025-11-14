package com.Zygo.proyecto.model;

import jakarta.persistence.*;

/**
 * Entidad que representa un nodo del grafo (ubicación)
 * Puede ser: restaurante, cliente, intersección, base de repartidores
 */
@Entity
@Table(name = "nodos_grafo")
public class Graph {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String nombre;
    
    @Column(nullable = false)
    private Double latitud;
    
    @Column(nullable = false)
    private Double longitud;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNodo tipo;
    
    @Column(name = "direccion_completa")
    private String direccionCompleta;
    
    private String descripcion;
    
    @Column(nullable = false)
    private Boolean activo = true;
    
    public enum TipoNodo {
        RESTAURANTE,
        CLIENTE,
        INTERSECCION,
        BASE_REPARTIDORES
    }
    
    // Constructores
    public Graph() {}
    
    public Graph(String nombre, Double latitud, Double longitud, TipoNodo tipo) {
        this.nombre = nombre;
        this.latitud = latitud;
        this.longitud = longitud;
        this.tipo = tipo;
        this.direccionCompleta = nombre;
    }
    
    /**
     * Calcula la distancia euclidiana aproximada a otro nodo
     * (Para simplificación, no usa fórmula de Haversine)
     */
    public double calcularDistancia(Graph otroNodo) {
        double deltaLat = this.latitud - otroNodo.latitud;
        double deltaLon = this.longitud - otroNodo.longitud;
        return Math.sqrt(deltaLat * deltaLat + deltaLon * deltaLon) * 111; // Aproximación a km
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
    
    public TipoNodo getTipo() {
        return tipo;
    }
    
    public void setTipo(TipoNodo tipo) {
        this.tipo = tipo;
    }
    
    public String getDireccionCompleta() {
        return direccionCompleta;
    }
    
    public void setDireccionCompleta(String direccionCompleta) {
        this.direccionCompleta = direccionCompleta;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public Boolean getActivo() {
        return activo;
    }
    
    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Graph graph = (Graph) o;
        return id != null && id.equals(graph.id);
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    @Override
    public String toString() {
        return "Graph{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", tipo=" + tipo +
                '}';
    }
}