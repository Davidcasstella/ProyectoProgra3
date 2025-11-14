-- Script para limpiar completamente el grafo
-- Ejecutar manualmente si es necesario

USE zygo_db;

-- Eliminar aristas
DELETE FROM aristas_grafo;

-- Eliminar nodos
DELETE FROM nodos_grafo;

-- Resetear auto_increment (opcional)
ALTER TABLE nodos_grafo AUTO_INCREMENT = 1;
ALTER TABLE aristas_grafo AUTO_INCREMENT = 1;

-- Verificar limpieza
SELECT 
    (SELECT COUNT(*) FROM nodos_grafo) as nodos,
    (SELECT COUNT(*) FROM aristas_grafo) as aristas;