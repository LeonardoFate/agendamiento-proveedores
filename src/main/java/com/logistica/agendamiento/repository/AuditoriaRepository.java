package com.logistica.agendamiento.repository;

import com.logistica.agendamiento.entity.Auditoria;
import com.logistica.agendamiento.entity.Usuario;
import com.logistica.agendamiento.entity.enums.TipoOperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findByTabla(String tabla);

    List<Auditoria> findByOperacion(TipoOperacion operacion);

    List<Auditoria> findByUsuario(Usuario usuario);

    List<Auditoria> findByCreatedAtBetween(LocalDateTime inicio, LocalDateTime fin);
}