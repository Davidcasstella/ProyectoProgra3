package com.Zygo.proyecto.repository;

import com.Zygo.proyecto.model.Pedido;
import com.Zygo.proyecto.model.Pedido.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    
    // 📦 Buscar por cliente
    List<Pedido> findByClienteId(Long clienteId);
    
    // 🚚 Buscar por repartidor
    List<Pedido> findByRepartidorId(Long repartidorId);
    
    // ✅ MÉTODO CRÍTICO: Buscar por repartidor y estado específico
    List<Pedido> findByRepartidorIdAndEstado(Long repartidorId, EstadoPedido estado);
    
    // 📊 Buscar por estado
    List<Pedido> findByEstado(EstadoPedido estado);
    
    // 🔥 Pedidos activos del repartidor (ASIGNADO o EN_CAMINO)
    @Query("SELECT p FROM Pedido p WHERE p.repartidor.id = :repartidorId AND " +
           "(p.estado = 'ASIGNADO' OR p.estado = 'EN_CAMINO')")
    List<Pedido> findPedidosActivosPorRepartidor(@Param("repartidorId") Long repartidorId);
    
    // 🔢 Contar pedidos activos del repartidor
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.repartidor.id = :repartidorId AND " +
           "(p.estado = 'ASIGNADO' OR p.estado = 'EN_CAMINO')")
    Long countPedidosActivosPorRepartidor(@Param("repartidorId") Long repartidorId);
    
    // 📋 Pedidos sin asignar (para que ADMIN los asigne)
    @Query("SELECT p FROM Pedido p WHERE p.estado = 'PENDIENTE' AND p.repartidor IS NULL")
    List<Pedido> findPedidosPendientesSinAsignar();
    
    // 📈 Contar pedidos por estado
    Long countByEstado(EstadoPedido estado);
    
    // 📊 Contar pedidos de un repartidor en estado específico
    Long countByRepartidorIdAndEstado(Long repartidorId, EstadoPedido estado);
}