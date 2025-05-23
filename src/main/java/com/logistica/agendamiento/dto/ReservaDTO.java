package com.logistica.agendamiento.dto;

import com.logistica.agendamiento.entity.enums.EstadoReserva;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ReservaDTO {

    private Long id;

    // ✅ CAMPOS QUE MANTIENE (para admin y proveedor)
    @NotNull(message = "El ID del proveedor es obligatorio")
    private Long proveedorId;

    private String proveedorNombre;

    @NotNull(message = "La fecha es obligatoria")
    @FutureOrPresent(message = "La fecha debe ser hoy o futura")
    private LocalDate fecha;

    private EstadoReserva estado;
    private String descripcion;

    // ✅ NUEVO: Número de palets (obligatorio para proveedor)
    @Positive(message = "El número de palets debe ser mayor a 0")
    private Integer numeroPalets;

    // ✅ CAMPOS QUE SOLO USA EL ADMIN (vienen de plantilla para proveedor)
    private Long areaId;
    private String areaNombre;
    private Long andenId;
    private Integer andenNumero;
    private Long tipoServicioId;
    private String tipoServicioNombre;
    private LocalTime horaInicio;
    private LocalTime horaFin;

    // ✅ DATOS DE TRANSPORTE (obligatorios para proveedor)
    @NotBlank(message = "El tipo de transporte es obligatorio")
    private String transporteTipo;

    @NotBlank(message = "La marca del transporte es obligatoria")
    private String transporteMarca;

    @NotBlank(message = "El modelo del transporte es obligatorio")
    private String transporteModelo;

    @NotBlank(message = "La placa del transporte es obligatoria")
    private String transportePlaca;

    private String transporteCapacidad;

    // ✅ DATOS DEL CONDUCTOR (obligatorios para proveedor)
    @NotBlank(message = "Los nombres del conductor son obligatorios")
    private String conductorNombres;

    @NotBlank(message = "Los apellidos del conductor son obligatorios")
    private String conductorApellidos;

    @NotBlank(message = "La cédula del conductor es obligatoria")
    private String conductorCedula;

    // ✅ DATOS DE LOS AYUDANTES (opcional para proveedor)
    private List<AyudanteDTO> ayudantes = new ArrayList<>();
}