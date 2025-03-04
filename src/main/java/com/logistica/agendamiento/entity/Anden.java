package com.logistica.agendamiento.entity;

import com.logistica.agendamiento.entity.enums.EstadoAnden;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "anden", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"area_id", "numero"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Anden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(nullable = false)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAnden estado = EstadoAnden.DISPONIBLE;

    private String capacidad;

    @Column(nullable = false)
    private Boolean exclusivoContenedor = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}