package com.logistica.agendamiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaDTO {

    private Long id;

    @NotBlank(message = "El nombre del área es obligatorio")
    @Size(max = 50, message = "El nombre del área no debe exceder los 50 caracteres")
    private String nombre;

    private String descripcion;
}