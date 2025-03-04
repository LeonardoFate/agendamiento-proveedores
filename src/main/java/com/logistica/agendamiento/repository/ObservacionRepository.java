package com.logistica.agendamiento.repository;

import com.logistica.agendamiento.entity.Observacion;
import com.logistica.agendamiento.entity.Reserva;
import com.logistica.agendamiento.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObservacionRepository extends JpaRepository<Observacion, Long> {

    List<Observacion> findByReserva(Reserva reserva);

    List<Observacion> findByUsuario(Usuario usuario);
}