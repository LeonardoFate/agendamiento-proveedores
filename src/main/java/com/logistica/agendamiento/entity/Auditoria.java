package com.logistica.agendamiento.entity;

import com.logistica.agendamiento.entity.enums.TipoOperacion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String tabla;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoOperacion operacion;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(columnDefinition = "TEXT")
    private String valorAntiguo;

    @Column(columnDefinition = "TEXT")
    private String valorNuevo;

    @Column(length = 50)
    private String ip;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}