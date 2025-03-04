package com.logistica.agendamiento.repository;

import com.logistica.agendamiento.entity.Transporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransporteRepository extends JpaRepository<Transporte, Long> {

    Optional<Transporte> findByPlaca(String placa);

    List<Transporte> findByTipo(String tipo);
}