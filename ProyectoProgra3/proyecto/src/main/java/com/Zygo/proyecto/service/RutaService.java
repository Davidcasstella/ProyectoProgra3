package com.Zygo.proyecto.service;

import com.Zygo.proyecto.model.Ruta;
import com.Zygo.proyecto.repository.RutaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class RutaService {
    
    private static final Logger log = LoggerFactory.getLogger(RutaService.class);
    
    @Autowired
    private RutaRepository rutaRepository;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 💾 Guardar ruta en la BD
     */
    public Ruta guardarRuta(Map<String, Object> rutaData) {
        try {
            log.info("💾 Guardando ruta...");
            
            Ruta ruta = new Ruta();
            
            // Coordinates
            ruta.setOrigenLatitud((Double) rutaData.get("origenLatitud"));
            ruta.setOrigenLongitud((Double) rutaData.get("origenLongitud"));
            ruta.setDestinoLatitud((Double) rutaData.get("destinoLatitud"));
            ruta.setDestinoLongitud((Double) rutaData.get("destinoLongitud"));
            
            // Distancia, tiempo, costo
            Object distancia = rutaData.get("distanciaKm");
            ruta.setDistanciaKm(distancia instanceof Number ? ((Number) distancia).doubleValue() : 0.0);
            
            Object tiempo = rutaData.get("tiempoEstimadoMinutos");
            ruta.setTiempoEstimadoMinutos(tiempo instanceof Number ? ((Number) tiempo).intValue() : 0);
            
            Object costo = rutaData.get("costoEstimado");
            ruta.setCostoEstimado(costo instanceof Number ? ((Number) costo).doubleValue() : 0.0);
            
            // Instrucciones
            ruta.setInstrucciones((String) rutaData.get("instrucciones"));
            
            // Nodos y segmentos como JSON
            if (rutaData.get("nodos") != null) {
                ruta.setNodos(objectMapper.writeValueAsString(rutaData.get("nodos")));
            }
            
            if (rutaData.get("segmentos") != null) {
                ruta.setSegmentos(objectMapper.writeValueAsString(rutaData.get("segmentos")));
            }
            
            // Pedido ID (opcional)
            Object pedidoId = rutaData.get("pedidoId");
            if (pedidoId != null) {
                ruta.setPedidoId(((Number) pedidoId).longValue());
            }
            
            ruta.setActivo(true);
            
            Ruta rutaGuardada = rutaRepository.save(ruta);
            
            log.info("✅ Ruta guardada con ID: {}", rutaGuardada.getId());
            
            return rutaGuardada;
            
        } catch (Exception e) {
            log.error("❌ Error al guardar ruta: {}", e.getMessage(), e);
            throw new RuntimeException("Error al guardar la ruta: " + e.getMessage());
        }
    }
    
    /**
     * 🔍 Obtener ruta por ID
     */
    public Ruta obtenerRuta(Long id) {
        return rutaRepository.findById(id).orElse(null);
    }
}