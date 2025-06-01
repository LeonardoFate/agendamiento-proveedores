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

// ReservaDTO.java - Modificado para que el proveedor complete todo

@Data
public class ReservaDTO {

    private Long id;

    // ✅ DATOS DE LA PLANTILLA (ya vienen llenos)
    @NotNull(message = "El ID del proveedor es obligatorio")
    private Long proveedorId;
    private String proveedorNombre;

    @NotNull(message = "La fecha es obligatoria")
    @FutureOrPresent(message = "La fecha debe ser hoy o futura")
    private LocalDate fecha;

    private LocalTime horaInicio;  // De la plantilla
    private LocalTime horaFin;     // De la plantilla
    private EstadoReserva estado;

    // 🔴 DATOS QUE EL PROVEEDOR DEBE COMPLETAR

    // === SELECCIÓN DE RECURSOS ===
    @NotNull(message = "Debe seleccionar un área")
    private Long areaId;
    private String areaNombre;

    @NotNull(message = "Debe seleccionar un andén")
    private Long andenId;
    private Integer andenNumero;

    @NotNull(message = "Debe seleccionar un tipo de servicio")
    private Long tipoServicioId;
    private String tipoServicioNombre;

    // === DATOS DE TRANSPORTE ===
    @NotBlank(message = "El tipo de transporte es obligatorio")
    private String transporteTipo;

    @NotBlank(message = "La marca del transporte es obligatoria")
    private String transporteMarca;

    @NotBlank(message = "El modelo del transporte es obligatorio")
    private String transporteModelo;

    @NotBlank(message = "La placa del transporte es obligatoria")
    private String transportePlaca;

    private String transporteCapacidad;

    // === DATOS DEL CONDUCTOR ===
    @NotBlank(message = "Los nombres del conductor son obligatorios")
    private String conductorNombres;

    @NotBlank(message = "Los apellidos del conductor son obligatorios")
    private String conductorApellidos;

    @NotBlank(message = "La cédula del conductor es obligatoria")
    private String conductorCedula;

    // === DATOS ADICIONALES ===
    @Positive(message = "El número de palets debe ser mayor a 0")
    private Integer numeroPalets;

    private String descripcion;

    // === AYUDANTES (OPCIONAL) ===
    private List<AyudanteDTO> ayudantes = new ArrayList<>();
}