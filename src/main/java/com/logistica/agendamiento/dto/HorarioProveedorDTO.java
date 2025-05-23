package com.logistica.agendamiento.dto;

import com.logistica.agendamiento.entity.enums.DiaSemana;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HorarioProveedorDTO {

    private LocalDate fecha;
    private DiaSemana dia;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private LocalTime tiempoDescarga;
    private Integer numeroPersonas;

    // Datos del lugar asignado
    private String areaNombre;
    private Integer andenNumero;
    private String tipoServicioNombre;

    // Estado de reserva
    private Boolean tieneReserva;
    private Long reservaId;
    private String estadoReserva;

    // Para mostrar si puede confirmar
    private Boolean puedeConfirmar;
}