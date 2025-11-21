package com.Zygo.proyecto.repository;

import com.Zygo.proyecto.model.HistorialRuta;
import com.Zygo.proyecto.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 📊 REPOSITORY: Historial de Rutas
 * 
 * Consultas especializadas para el historial de cálculo de rutas
 */
@Repository
public interface HistorialRutaRepository extends JpaRepository<HistorialRuta, Long> {
    
    /**
     * 🔍 Buscar historial por pedido (activos)
     */
    List<HistorialRuta> findByPedidoIdAndActivoTrue(Long pedidoId);
    
    /**
     * 🔍 Buscar historial por repartidor
     */
    List<HistorialRuta> findByRepartidorAndActivoTrueOrderByFechaCalculoDesc(Usuario repartidor);
    
    /**
     * 🔍 Buscar por tipo de cálculo
     */
    List<HistorialRuta> findByTipoCalculoAndActivoTrueOrderByFechaCalculoDesc(
            HistorialRuta.TipoCalculo tipoCalculo
    );
    
    /**
     * 🔍 Últimas 50 rutas calculadas (para dashboard admin)
     */
    List<HistorialRuta> findTop50ByActivoTrueOrderByFechaCalculoDesc();
    
    /**
     * 📊 Contar rutas calculadas hoy
     */
    @Query("SELECT COUNT(h) FROM HistorialRuta h WHERE " +
           "DATE(h.fechaCalculo) = CURRENT_DATE AND h.activo = true")
    long countRutasCalculadasHoy();
    
    /**
     * 📊 Contar rutas por restaurante
     */
    @Query("SELECT COUNT(h) FROM HistorialRuta h WHERE " +
           "h.restaurante.id = :restauranteId AND h.activo = true")
    long countByRestauranteId(@Param("restauranteId") Long restauranteId);
    
    /**
     * 📊 Obtener historial entre fechas
     */
    @Query("SELECT h FROM HistorialRuta h WHERE " +
           "h.fechaCalculo BETWEEN :inicio AND :fin AND h.activo = true " +
           "ORDER BY h.fechaCalculo DESC")
    List<HistorialRuta> findByFechaCalculoBetween(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );
    
    /**
     * 📊 Rutas más largas (top 10)
     */
    @Query("SELECT h FROM HistorialRuta h WHERE h.activo = true " +
           "ORDER BY h.distanciaTotalKm DESC")
    List<HistorialRuta> findTop10ByOrderByDistanciaTotalKmDesc();
    
    /**
     * 📊 Rutas más rápidas calculadas (por tiempo de cálculo)
     */
    @Query("SELECT h FROM HistorialRuta h WHERE h.activo = true " +
           "ORDER BY h.tiempoCalculoMs ASC")
    List<HistorialRuta> findTop10ByOrderByTiempoCalculoMsAsc();
    
    /**
     * 📊 Promedio de distancia por tipo de cálculo
     */
    @Query("SELECT h.tipoCalculo, AVG(h.distanciaTotalKm) " +
           "FROM HistorialRuta h WHERE h.activo = true " +
           "GROUP BY h.tipoCalculo")
    List<Object[]> getPromedioDistanciaPorTipo();
    
    /**
     * 🔍 Buscar historial por visibilidad
     */
    List<HistorialRuta> findByVisibilidadAndActivoTrue(
            HistorialRuta.VisibilidadHistorial visibilidad
    );
}