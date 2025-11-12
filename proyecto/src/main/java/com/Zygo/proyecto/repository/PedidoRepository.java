package com.Zygo.proyecto.repository;

import com.Zygo.proyecto.model.Pedido;
import com.Zygo.proyecto.model.Pedido.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    
    List<Pedido> findByClienteId(Long clienteId);
    
    List<Pedido> findByRepartidorId(Long repartidorId);
    
    List<Pedido> findByEstado(EstadoPedido estado);
    
    @Query("SELECT p FROM Pedido p WHERE p.estado = :estado ORDER BY p.fechaCreacion ASC")
    List<Pedido> findPedidosPendientesOrdenados(EstadoPedido estado);
    
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.repartidor.id = :repartidorId AND p.estado IN ('ASIGNADO', 'EN_CAMINO')")
    Long countPedidosActivosPorRepartidor(Long repartidorId);
}