package com.Zygo.proyecto.repository;

import com.Zygo.proyecto.model.Graph;
import com.Zygo.proyecto.model.Graph.TipoNodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones con nodos del grafo
 */
@Repository
public interface GraphRepository extends JpaRepository<Graph, Long> {
    
    Optional<Graph> findByNombre(String nombre);
    
    List<Graph> findByTipo(TipoNodo tipo);
    
    @Query("SELECT g FROM Graph g WHERE g.latitud BETWEEN :latMin AND :latMax AND g.longitud BETWEEN :lonMin AND :lonMax")
    List<Graph> findNodosEnArea(@Param("latMin") Double latMin, 
                                 @Param("latMax") Double latMax,
                                 @Param("lonMin") Double lonMin, 
                                 @Param("lonMax") Double lonMax);
    
    @Query("SELECT g FROM Graph g WHERE " +
           "SQRT(POWER(g.latitud - :lat, 2) + POWER(g.longitud - :lon, 2)) <= :radio")
    List<Graph> findNodosCercanos(@Param("lat") Double latitud, 
                                   @Param("lon") Double longitud, 
                                   @Param("radio") Double radio);
    
    @Query("SELECT g FROM Graph g WHERE g.tipo = :tipo AND " +
           "SQRT(POWER(g.latitud - :lat, 2) + POWER(g.longitud - :lon, 2)) <= :radio " +
           "ORDER BY SQRT(POWER(g.latitud - :lat, 2) + POWER(g.longitud - :lon, 2))")
    List<Graph> findNodosCercanosPorTipo(@Param("lat") Double latitud, 
                                          @Param("lon") Double longitud, 
                                          @Param("radio") Double radio,
                                          @Param("tipo") TipoNodo tipo);
}