package com.Zygo.proyecto.security;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.Zygo.proyecto.model.Usuario;
import com.Zygo.proyecto.repository.UsuarioRepository;

import java.util.Collection;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));
        
        return new org.springframework.security.core.userdetails.User(
                usuario.getEmail(),
                usuario.getPassword(),
                usuario.getActivo(),
                true,
                true,
                true,
                obtenerAutoridades(usuario)
        );
    }
    
    private Collection<? extends GrantedAuthority> obtenerAutoridades(Usuario usuario) {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + usuario.getTipo().name())
        );
    }
}