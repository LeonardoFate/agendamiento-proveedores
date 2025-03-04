package com.logistica.agendamiento.repository;

import com.logistica.agendamiento.entity.Documento;
import com.logistica.agendamiento.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    List<Documento> findByReserva(Reserva reserva);

    List<Documento> findByTipo(String tipo);
}