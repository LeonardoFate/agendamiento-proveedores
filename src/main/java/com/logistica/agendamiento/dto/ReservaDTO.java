package com.logistica.agendamiento.dto;

import com.logistica.agendamiento.entity.enums.EstadoReserva;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ReservaDTO {

    private Long id;

    @NotNull(message = "El ID del proveedor es obligatorio")
    private Long proveedorId;

    private String proveedorNombre;

    private String areaNombre;

    private Integer andenNumero;

    private String tipoServicioNombre;

    @NotNull(message = "La fecha es obligatoria")
    @FutureOrPresent(message = "La fecha debe ser hoy o futura")
    private LocalDate fecha;

    private EstadoReserva estado;

    private String descripcion;

    // Datos del transporte
    @NotBlank(message = "El tipo de transporte es obligatorio")
    private String transporteTipo;

    @NotBlank(message = "La marca del transporte es obligatoria")
    private String transporteMarca;

    @NotBlank(message = "El modelo del transporte es obligatorio")
    private String transporteModelo;

    @NotBlank(message = "La placa del transporte es obligatoria")
    private String transportePlaca;

    private String transporteCapacidad;

    // Datos del conductor
    @NotBlank(message = "Los nombres del conductor son obligatorios")
    private String conductorNombres;

    @NotBlank(message = "Los apellidos del conductor son obligatorios")
    private String conductorApellidos;

    @NotBlank(message = "La cédula del conductor es obligatoria")
    private String conductorCedula;

    // Datos de los ayudantes
    private List<AyudanteDTO> ayudantes = new ArrayList<>();
}