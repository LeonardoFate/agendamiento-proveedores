package com.logistica.agendamiento.repository;

import com.logistica.agendamiento.entity.Anden;
import com.logistica.agendamiento.entity.Area;
import com.logistica.agendamiento.entity.enums.EstadoAnden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AndenRepository extends JpaRepository<Anden, Long> {

    List<Anden> findByArea(Area area);

    List<Anden> findByAreaAndEstado(Area area, EstadoAnden estado);

    List<Anden> findByAreaAndExclusivoContenedor(Area area, Boolean exclusivoContenedor);

    Optional<Anden> findByAreaAndNumero(Area area, Integer numero);

    boolean existsByAreaAndNumero(Area area, Integer numero);
}