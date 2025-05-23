package com.logistica.agendamiento.repository;

import com.logistica.agendamiento.entity.Transporte;
import com.logistica.agendamiento.entity.Transportista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransportistaRepository extends JpaRepository<Transportista, Long> {

    List<Transportista> findByTransporte(Transporte transporte);

    List<Transportista> findByEsConductorTrue();

    Optional<Transportista> findByCedula(String cedula);

    void deleteAllByTransporte(Transporte transporte);

}