package com.Zygo.proyecto.repository;

import com.Zygo.proyecto.model.Usuario;
import com.Zygo.proyecto.model.Usuario.TipoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    Optional<Usuario> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    // ✅ NUEVO: Contar usuarios por tipo
    long countByTipo(TipoUsuario tipo);
    
    // ✅ MEJORADO: Método más genérico
    List<Usuario> findByTipo(TipoUsuario tipo);
    
    @Query("SELECT u FROM Usuario u WHERE u.tipo = :tipo AND u.activo = true")
    List<Usuario> findRepartidoresActivos(@Param("tipo") TipoUsuario tipo);
    
    // ✅ NUEVO: Encontrar repartidores disponibles
    @Query("SELECT u FROM Usuario u WHERE u.tipo = 'REPARTIDOR' AND u.activo = true AND u.disponible = true")
    List<Usuario> findRepartidoresDisponibles();
    
    // ✅ NUEVO: Encontrar repartidor más cercano a una ubicación
    @Query("SELECT u FROM Usuario u WHERE u.tipo = 'REPARTIDOR' AND u.activo = true AND u.disponible = true " +
           "ORDER BY SQRT(POWER(u.latitud - :lat, 2) + POWER(u.longitud - :lon, 2))")
    List<Usuario> findRepartidoresCercanos(@Param("lat") Double latitud, @Param("lon") Double longitud);
}