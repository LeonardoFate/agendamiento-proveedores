package com.logistica.agendamiento.dto;

import com.logistica.agendamiento.entity.enums.EstadoAnden;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DisponibilidadAndenDTO {

    private Long andenId;
    private Integer numero;
    private Long areaId;
    private String areaNombre;
    private EstadoAnden estadoActual;
    private Boolean exclusivoContenedor;
    private List<HorarioReservadoDTO> horariosReservados = new ArrayList<>();
}