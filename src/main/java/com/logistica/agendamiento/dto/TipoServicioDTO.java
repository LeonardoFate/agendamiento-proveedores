package com.logistica.agendamiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoServicioDTO {

    private Long id;

    @NotBlank(message = "El nombre del tipo de servicio es obligatorio")
    @Size(max = 50, message = "El nombre no debe exceder los 50 caracteres")
    private String nombre;

    private String descripcion;
}