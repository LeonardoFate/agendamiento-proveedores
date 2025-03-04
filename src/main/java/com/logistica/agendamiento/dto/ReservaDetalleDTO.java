package com.logistica.agendamiento.dto;

import com.logistica.agendamiento.entity.enums.EstadoReserva;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ReservaDetalleDTO {

    private Long id;
    private Long proveedorId;
    private String proveedorNombre;
    private Long areaId;
    private String areaNombre;
    private Long andenId;
    private Integer andenNumero;
    private Long tipoServicioId;
    private String tipoServicioNombre;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private EstadoReserva estado;
    private String descripcion;

    // Datos del transporte
    private String transporteTipo;
    private String transporteMarca;
    private String transporteModelo;
    private String transportePlaca;
    private String transporteCapacidad;

    // Datos del conductor
    private String conductorNombres;
    private String conductorApellidos;
    private String conductorCedula;

    // Datos de los ayudantes
    private List<AyudanteDTO> ayudantes = new ArrayList<>();

    // Fechas de auditoría
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}