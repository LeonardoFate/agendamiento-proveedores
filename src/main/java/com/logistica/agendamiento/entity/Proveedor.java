package com.logistica.agendamiento.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "proveedor")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(unique = true, nullable = false, length = 20)
    private String ruc;

    @Column(nullable = false)
    private String direccion;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private Boolean estado = true;

    @OneToOne
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    // CAMPO COMENTADO: La columna acepto_politica_privacidad no existe en la BD
    // Solo existen: fecha_aceptacion_politica e ip_aceptacion_politica
    // @Column(name = "acepto_politica_privacidad", nullable = false)
    // private Boolean aceptoPoliticaPrivacidad = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "fecha_aceptacion_politica")
    private LocalDateTime fechaAceptacionPolitica;

    @Column(name = "ip_aceptacion_politica", length = 50)
    private String ipAceptacionPolitica;
}