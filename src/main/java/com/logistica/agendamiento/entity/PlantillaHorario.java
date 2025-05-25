package com.logistica.agendamiento.entity;

import com.logistica.agendamiento.entity.enums.DiaSemana;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "plantilla_horario")
// ✅ IMPORTANTE: Removido el constraint único que causaba problemas
// La validación ahora se hace en el servicio para mayor flexibilidad
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiaSemana dia;

    @ManyToOne(fetch = FetchType.LAZY) // ✅ Mejorado para performance
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(nullable = false)
    private Integer numeroPersonas;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false)
    private LocalTime tiempoDescarga;

    @ManyToOne(fetch = FetchType.LAZY) // ✅ Mejorado para performance
    @JoinColumn(name = "area_id")
    private Area area;

    @ManyToOne(fetch = FetchType.LAZY) // ✅ Mejorado para performance
    @JoinColumn(name = "anden_id")
    private Anden anden;

    @ManyToOne(fetch = FetchType.LAZY) // ✅ Mejorado para performance
    @JoinColumn(name = "tipo_servicio_id")
    private TipoServicio tipoServicio;

    @Column(nullable = false)
    private Boolean activo = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ===== MÉTODOS ESENCIALES PARA GESTIÓN DE CONFLICTOS =====

    /**
     * Desactiva esta plantilla (soft delete)
     */
    public void desactivar() {
        this.activo = false;
    }

    /**
     * Reactiva esta plantilla
     */
    public void reactivar() {
        this.activo = true;
    }

    /**
     * Verifica si la plantilla está activa
     */
    public boolean isActiva() {
        return Boolean.TRUE.equals(this.activo);
    }

    /**
     * Verifica si dos plantillas son del mismo proveedor y día
     */
    public boolean mismoProgramacion(PlantillaHorario otra) {
        return this.proveedor.getId().equals(otra.proveedor.getId()) &&
                this.dia.equals(otra.dia);
    }

    /**
     * Verifica si dos plantillas tienen horarios similares
     * Útil para la reactivación inteligente
     */
    public boolean horariosSimilares(PlantillaHorario otra) {
        return this.horaInicio.equals(otra.horaInicio) &&
                this.horaFin.equals(otra.horaFin) &&
                this.numeroPersonas.equals(otra.numeroPersonas);
    }

    /**
     * Copia los datos de horario de otra plantilla
     * Útil para actualizaciones durante reactivación
     */
    public void copiarDatosHorario(PlantillaHorario origen) {
        this.numeroPersonas = origen.numeroPersonas;
        this.horaInicio = origen.horaInicio;
        this.horaFin = origen.horaFin;
        this.tiempoDescarga = origen.tiempoDescarga;
    }

    /**
     * Genera una clave única para identificar plantillas del mismo proveedor/día
     */
    public String getClaveUnica() {
        return proveedor.getId() + "_" + dia.name();
    }

    /**
     * Verifica si la plantilla tiene recursos asignados
     */
    public boolean tieneRecursosAsignados() {
        return area != null && anden != null && tipoServicio != null;
    }

    // ===== OVERRIDE EQUALS Y HASHCODE PARA EVITAR PROBLEMAS EN COLECCIONES =====

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlantillaHorario)) return false;

        PlantillaHorario that = (PlantillaHorario) o;

        // Si ambos tienen ID, comparar por ID
        if (id != null && that.id != null) {
            return id.equals(that.id);
        }

        // Para entidades nuevas, comparar por campos únicos
        return proveedor != null &&
                proveedor.equals(that.proveedor) &&
                dia != null &&
                dia.equals(that.dia) &&
                Boolean.TRUE.equals(activo) &&
                Boolean.TRUE.equals(that.activo);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }

        // Para entidades nuevas
        int result = proveedor != null ? proveedor.hashCode() : 0;
        result = 31 * result + (dia != null ? dia.hashCode() : 0);
        result = 31 * result + (Boolean.TRUE.equals(activo) ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PlantillaHorario{" +
                "id=" + id +
                ", dia=" + dia +
                ", proveedor=" + (proveedor != null ? proveedor.getNombre() : "null") +
                ", numeroPersonas=" + numeroPersonas +
                ", horaInicio=" + horaInicio +
                ", horaFin=" + horaFin +
                ", activo=" + activo +
                '}';
    }
}