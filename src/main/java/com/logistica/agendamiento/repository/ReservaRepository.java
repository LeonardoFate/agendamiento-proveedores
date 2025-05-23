package com.logistica.agendamiento.repository;

import com.logistica.agendamiento.entity.*;
import com.logistica.agendamiento.entity.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByProveedor(Proveedor proveedor);

    List<Reserva> findByArea(Area area);

    List<Reserva> findByAnden(Anden anden);

    List<Reserva> findByTipoServicio(TipoServicio tipoServicio);

    List<Reserva> findByEstado(EstadoReserva estado);

    List<Reserva> findByFecha(LocalDate fecha);

    List<Reserva> findByFechaAndAnden(LocalDate fecha, Anden anden);

    List<Reserva> findByFechaAndArea(LocalDate fecha, Area area);

    @Query("SELECT r FROM Reserva r WHERE r.fecha = :fecha AND r.estado = :estado")
    List<Reserva> findByFechaAndEstado(@Param("fecha") LocalDate fecha, @Param("estado") EstadoReserva estado);

    @Query("SELECT DISTINCT r.fecha FROM Reserva r WHERE r.fecha >= :fechaInicio AND r.fecha <= :fechaFin")
    List<LocalDate> findFechasConReservas(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin);
}