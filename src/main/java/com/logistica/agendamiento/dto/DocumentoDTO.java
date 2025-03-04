package com.logistica.agendamiento.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentoDTO {

    private Long id;
    private Long reservaId;
    private String nombre;
    private String ruta;
    private String tipo;
    private Long tamano;
    private String descripcion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}