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
    
    List<Pedido> findByClienteId(Long clienteId);
    
    List<Pedido> findByRepartidorId(Long repartidorId);
    
    List<Pedido> findByEstado(EstadoPedido estado);
    
    @Query("SELECT p FROM Pedido p WHERE p.repartidor.id = :repartidorId AND " +
           "(p.estado = 'ASIGNADO' OR p.estado = 'EN_CAMINO')")
    List<Pedido> findPedidosActivosPorRepartidor(@Param("repartidorId") Long repartidorId);
    
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.repartidor.id = :repartidorId AND " +
           "(p.estado = 'ASIGNADO' OR p.estado = 'EN_CAMINO')")
    Long countPedidosActivosPorRepartidor(@Param("repartidorId") Long repartidorId);
}