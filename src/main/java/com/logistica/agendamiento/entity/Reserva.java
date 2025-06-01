package com.logistica.agendamiento.entity;

import com.logistica.agendamiento.entity.enums.EstadoReserva;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reserva", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"anden_id", "fecha", "hora_inicio"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "proveedor_id", nullable = false) // ✅ Este SÍ es obligatorio
    private Proveedor proveedor;

    @ManyToOne
    @JoinColumn(name = "area_id", nullable = true) // ✅ CAMBIO: Permitir NULL para PRE-RESERVAS
    private Area area;

    @ManyToOne
    @JoinColumn(name = "anden_id", nullable = true) // ✅ CAMBIO: Permitir NULL para PRE-RESERVAS
    private Anden anden;

    @ManyToOne
    @JoinColumn(name = "tipo_servicio_id", nullable = true) // ✅ CAMBIO: Permitir NULL para PRE-RESERVAS
    private TipoServicio tipoServicio;

    @ManyToOne
    @JoinColumn(name = "transporte_id", nullable = true) // ✅ CAMBIO: Permitir NULL para PRE-RESERVAS
    private Transporte transporte;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado = EstadoReserva.PENDIENTE_CONFIRMACION; // ✅ Estado por defecto

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Documento> documentos = new ArrayList<>();

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegistroTiempo> registrosTiempo = new ArrayList<>();

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Observacion> observaciones = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}