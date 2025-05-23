package com.logistica.agendamiento.repository;

import com.logistica.agendamiento.entity.PlantillaHorario;
import com.logistica.agendamiento.entity.Proveedor;
import com.logistica.agendamiento.entity.enums.DiaSemana;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlantillaHorarioRepository extends JpaRepository<PlantillaHorario, Long> {

    Optional<PlantillaHorario> findByProveedorAndDiaAndActivoTrue(Proveedor proveedor, DiaSemana dia);

    List<PlantillaHorario> findByProveedorAndActivoTrue(Proveedor proveedor);

    List<PlantillaHorario> findByDiaAndActivoTrue(DiaSemana dia);

    List<PlantillaHorario> findByActivoTrue();

    boolean existsByProveedorAndDiaAndActivoTrue(Proveedor proveedor, DiaSemana dia);

    @Query("SELECT p FROM PlantillaHorario p WHERE p.activo = true ORDER BY p.dia, p.horaInicio")
    List<PlantillaHorario> findAllActivasOrdenadas();
}
