package com.logistica.agendamiento.dto;

import com.logistica.agendamiento.entity.enums.TipoRegistro;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegistroTiempoDTO {

    private Long id;
    private Long reservaId;
    private String proveedorNombre;
    private Long usuarioId;
    private String usuarioNombre;
    private TipoRegistro tipo;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;
    private Integer duracion;
}