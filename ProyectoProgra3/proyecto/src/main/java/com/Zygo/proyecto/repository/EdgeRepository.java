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
 * Repositorio para operaciones con aristas del grafo
 */
@Repository
public interface EdgeRepository extends JpaRepository<Edge, Long> {
    
    List<Edge> findByNodoOrigen(Graph nodoOrigen);
    
    List<Edge> findByNodoDestino(Graph nodoDestino);
    
    List<Edge> findByNodoOrigenAndActivoTrue(Graph nodoOrigen);
    
    List<Edge> findByNodoDestinoAndActivoTrue(Graph nodoDestino);
    
    Optional<Edge> findByNodoOrigenAndNodoDestino(Graph nodoOrigen, Graph nodoDestino);
    
    Optional<Edge> findByNodoOrigenAndNodoDestinoAndActivoTrue(Graph nodoOrigen, Graph nodoDestino);
    
    List<Edge> findByEsBidireccionalTrue();
    
    List<Edge> findByNodoDestinoAndEsBidireccionalTrueAndActivoTrue(Graph nodoDestino);
    
    @Query("SELECT e FROM Edge e WHERE e.factorTrafico > :umbral AND e.activo = true")
    List<Edge> findAristasConTrafico(@Param("umbral") Double umbral);
    
    @Query("SELECT e FROM Edge e WHERE e.tipoCalle = :tipo AND e.activo = true")
    List<Edge> findByTipoCalle(@Param("tipo") Edge.TipoCalle tipo);
    
    @Query("SELECT AVG(e.factorTrafico) FROM Edge e WHERE e.activo = true")
    Double obtenerPromedioTrafico();
    
    @Query("UPDATE Edge e SET e.factorTrafico = :factor WHERE e.id = :id")
    void actualizarFactorTrafico(@Param("id") Long id, @Param("factor") Double factor);
}