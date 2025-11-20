package com.Zygo.proyecto.service;

import com.Zygo.proyecto.dto.UsuarioDTO;
import com.Zygo.proyecto.model.Usuario;
import com.Zygo.proyecto.model.Usuario.TipoUsuario;
import com.Zygo.proyecto.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {
    
    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Transactional
    public UsuarioDTO crearUsuario(UsuarioDTO dto) {
        log.info("Creando usuario con email: {}", dto.getEmail());
        
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            log.error("El email {} ya existe", dto.getEmail());
            throw new RuntimeException("El email ya está registrado");
        }
        
        Usuario usuario = convertirDtoAEntidad(dto);
        Usuario guardado = usuarioRepository.save(usuario);
        
        log.info("Usuario creado exitosamente con ID: {}", guardado.getId());
        return convertirEntidadADto(guardado);
    }
    
    @Transactional(readOnly = true)
    public UsuarioDTO obtenerUsuarioPorId(Long id) {
        log.debug("Buscando usuario con ID: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return convertirEntidadADto(usuario);
    }
    
    @Transactional(readOnly = true)
    public List<UsuarioDTO> obtenerTodosLosUsuarios() {
        log.debug("Obteniendo todos los usuarios");
        return usuarioRepository.findAll().stream()
                .map(this::convertirEntidadADto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<UsuarioDTO> obtenerRepartidoresDisponibles() {
        log.debug("Buscando repartidores disponibles");
        return usuarioRepository.findRepartidoresActivos(TipoUsuario.REPARTIDOR).stream()
                .map(this::convertirEntidadADto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public UsuarioDTO actualizarUsuario(Long id, UsuarioDTO dto) {
        log.info("Actualizando usuario con ID: {}", id);
        
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        usuario.setNombre(dto.getNombre());
        usuario.setTelefono(dto.getTelefono());
        usuario.setDireccion(dto.getDireccion());
        usuario.setActivo(dto.getActivo());
        
        Usuario actualizado = usuarioRepository.save(usuario);
        log.info("Usuario actualizado exitosamente");
        
        return convertirEntidadADto(actualizado);
    }
    
    @Transactional
    public void eliminarUsuario(Long id) {
        log.info("Eliminando usuario con ID: {}", id);
        
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        
        log.info("Usuario desactivado exitosamente");
    }
    
    private Usuario convertirDtoAEntidad(UsuarioDTO dto) {
        Usuario usuario = new Usuario();
        usuario.setId(dto.getId());
        usuario.setNombre(dto.getNombre());
        usuario.setEmail(dto.getEmail());
        usuario.setTelefono(dto.getTelefono());
        usuario.setDireccion(dto.getDireccion());
        usuario.setTipo(dto.getTipo());
        usuario.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        return usuario;
    }
    
    private UsuarioDTO convertirEntidadADto(Usuario usuario) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setEmail(usuario.getEmail());
        dto.setTelefono(usuario.getTelefono());
        dto.setDireccion(usuario.getDireccion());
        dto.setTipo(usuario.getTipo());
        dto.setFechaRegistro(usuario.getFechaRegistro());
        dto.setActivo(usuario.getActivo());
        return dto;
    }
    // ✅ NUEVO: Obtener repartidores CON sus coordenadas
@Transactional(readOnly = true)
public List<UsuarioDTO> obtenerRepartidoresConUbicacion() {
    log.debug("🚴 Buscando repartidores con ubicación");
    return usuarioRepository.findByTipo(TipoUsuario.REPARTIDOR).stream()
            .filter(Usuario::getActivo)
            .map(this::convertirEntidadADtoConUbicacion)
            .collect(Collectors.toList());
}

// ✅ NUEVO: Convertir entidad a DTO incluyendo coordenadas
private UsuarioDTO convertirEntidadADtoConUbicacion(Usuario usuario) {
    UsuarioDTO dto = new UsuarioDTO();
    dto.setId(usuario.getId());
    dto.setNombre(usuario.getNombre());
    dto.setEmail(usuario.getEmail());
    dto.setTelefono(usuario.getTelefono());
    dto.setDireccion(usuario.getDireccion());
    dto.setTipo(usuario.getTipo());
    dto.setFechaRegistro(usuario.getFechaRegistro());
    dto.setActivo(usuario.getActivo());
    dto.setLatitud(usuario.getLatitud());      // ✅ NUEVO
    dto.setLongitud(usuario.getLongitud());    // ✅ NUEVO
    dto.setDisponible(usuario.getDisponible()); // ✅ NUEVO
    return dto;
}
}