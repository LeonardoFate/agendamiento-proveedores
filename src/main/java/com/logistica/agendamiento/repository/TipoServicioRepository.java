package com.logistica.agendamiento.repository;

import com.logistica.agendamiento.entity.TipoServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoServicioRepository extends JpaRepository<TipoServicio, Long> {

    Optional<TipoServicio> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}