package com.logistica.agendamiento.dto;

import com.logistica.agendamiento.entity.enums.EstadoAnden;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AndenDTO {

    private Long id;

    @NotNull(message = "El ID del área es obligatorio")
    private Long areaId;

    private String areaNombre;

    @NotNull(message = "El número de andén es obligatorio")
    @Positive(message = "El número de andén debe ser positivo")
    private Integer numero;

    @NotNull(message = "El estado del andén es obligatorio")
    private EstadoAnden estado = EstadoAnden.DISPONIBLE;

    private String capacidad;

    @NotNull(message = "Debe especificar si es exclusivo para contenedores")
    private Boolean exclusivoContenedor = false;
}