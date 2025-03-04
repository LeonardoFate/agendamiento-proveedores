package com.logistica.agendamiento.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HorarioReservadoDTO {

    private LocalTime horaInicio;
    private LocalTime horaFin;
}