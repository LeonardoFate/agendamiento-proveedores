package com.logistica.agendamiento.repository;

import com.logistica.agendamiento.entity.RegistroTiempo;
import com.logistica.agendamiento.entity.Reserva;
import com.logistica.agendamiento.entity.Usuario;
import com.logistica.agendamiento.entity.enums.TipoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistroTiempoRepository extends JpaRepository<RegistroTiempo, Long> {

    List<RegistroTiempo> findByReserva(Reserva reserva);

    List<RegistroTiempo> findByUsuario(Usuario usuario);

    List<RegistroTiempo> findByTipo(TipoRegistro tipo);

    Optional<RegistroTiempo> findByReservaAndTipoAndHoraFinIsNull(Reserva reserva, TipoRegistro tipo);

    @Query("SELECT rt FROM RegistroTiempo rt WHERE rt.horaInicio >= :fechaInicio AND rt.horaInicio <= :fechaFin")
    List<RegistroTiempo> findByRangoFechas(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    @Query("SELECT AVG(rt.duracion) FROM RegistroTiempo rt WHERE rt.tipo = :tipo AND rt.duracion IS NOT NULL")
    Double findPromedioDuracionByTipo(@Param("tipo") TipoRegistro tipo);
}