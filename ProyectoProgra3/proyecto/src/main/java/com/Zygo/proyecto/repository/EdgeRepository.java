package com.Zygo.proyecto.repository;

import com.Zygo.proyecto.model.Edge;
import com.Zygo.proyecto.model.Graph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 🚀 Repositorio OPTIMIZADO para operaciones con aristas
 */
@Repository
public interface EdgeRepository extends JpaRepository<Edge, Long> {
    
    // ============ MÉTODOS ORIGINALES ============
    
    List<Edge> findByNodoOrigenAndActivoTrue(Graph nodoOrigen);
    
    List<Edge> findByNodoDestinoAndEsBidireccionalTrueAndActivoTrue(Graph nodoDestino);
    
    Optional<Edge> findByNodoOrigenAndNodoDestinoAndActivoTrue(Graph nodoOrigen, Graph nodoDestino);
    
    @Query("SELECT AVG(e.factorTrafico) FROM Edge e WHERE e.activo = true")
    Double obtenerPromedioTrafico();
    
    @Query("SELECT e FROM Edge e WHERE e.factorTrafico >= :umbral AND e.activo = true")
    List<Edge> findAristasConTrafico(@Param("umbral") Double umbral);
    
    // ============ NUEVOS MÉTODOS OPTIMIZADOS ============
    
    /**
     * 🚀 CRÍTICO: Obtiene TODAS las aristas relevantes en UNA SOLA QUERY
     * Esto elimina el problema N+1 y acelera enormemente Dijkstra
     */
    @Query("SELECT e FROM Edge e WHERE " +
           "(e.nodoOrigen.id IN :nodosIds OR e.nodoDestino.id IN :nodosIds) " +
           "AND e.activo = true")
    List<Edge> findAristasPorNodos(@Param("nodosIds") List<Long> nodosIds);
    
    /**
     * 🚀 Obtiene aristas en un área geográfica específica
     * Útil para limitar la búsqueda a zonas relevantes
     */
    @Query("SELECT e FROM Edge e WHERE " +
           "e.activo = true AND " +
           "e.nodoOrigen.latitud BETWEEN :latMin AND :latMax AND " +
           "e.nodoOrigen.longitud BETWEEN :lonMin AND :lonMax")
    List<Edge> findAristasEnArea(
            @Param("latMin") Double latMin,
            @Param("latMax") Double latMax,
            @Param("lonMin") Double lonMin,
            @Param("lonMax") Double lonMax
    );
    
    /**
     * 🚀 Cuenta aristas activas (para monitoreo)
     */
    @Query("SELECT COUNT(e) FROM Edge e WHERE e.activo = true")
    Long countAristasActivas();
    
    /**
     * 🚀 Obtiene aristas con alto tráfico para análisis
     */
    @Query("SELECT e FROM Edge e WHERE " +
           "e.activo = true AND e.factorTrafico > 1.5 " +
           "ORDER BY e.factorTrafico DESC")
    List<Edge> findAristasCongesionadas();
}