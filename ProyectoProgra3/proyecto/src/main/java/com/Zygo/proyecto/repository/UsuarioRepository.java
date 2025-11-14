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
    
    @Query("SELECT u FROM Usuario u WHERE u.tipo = :tipo AND u.activo = true")
    List<Usuario> findRepartidoresActivos(@Param("tipo") TipoUsuario tipo);
}