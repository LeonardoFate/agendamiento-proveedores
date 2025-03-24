package com.logistica.agendamiento.repository;

import com.logistica.agendamiento.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByRuc(String ruc);

    Optional<Proveedor> findByEmail(String email);

    List<Proveedor> findByEstadoTrue();

    boolean existsByRuc(String ruc);

    boolean existsByEmail(String email);

    Optional<Proveedor> findByUsuarioId(Long usuarioId);
}