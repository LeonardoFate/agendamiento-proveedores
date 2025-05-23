package com.logistica.agendamiento.dto;

import com.logistica.agendamiento.entity.enums.DiaSemana;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaHorarioDTO {

    private Long id;

    @NotNull(message = "El día es obligatorio")
    private DiaSemana dia;

    @NotNull(message = "El ID del proveedor es obligatorio")
    private Long proveedorId;

    private String proveedorNombre;

    @NotNull(message = "El número de personas es obligatorio")
    @Positive(message = "El número de personas debe ser positivo")
    private Integer numeroPersonas;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;

    @NotNull(message = "El tiempo de descarga es obligatorio")
    private LocalTime tiempoDescarga;

    private Long areaId;
    private String areaNombre;

    private Long andenId;
    private Integer andenNumero;

    private Long tipoServicioId;
    private String tipoServicioNombre;

    private Boolean activo = true;
}