package com.logistica.agendamiento.scheduler;

import com.logistica.agendamiento.service.PlantillaHorarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservaScheduler {

    private final PlantillaHorarioService plantillaHorarioService;

    // Generar reservas automáticamente todos los días a las 23:00 para el día siguiente
    @Scheduled(cron = "* 10 * * * *") // 23:00 todos los días
    public void generarReservasDelDiaSiguiente() {
        try {
            LocalDate manana = LocalDate.now().plusDays(1);
            plantillaHorarioService.generarReservasDesdeePlantillas(manana);
            log.info("Reservas automáticas generadas para {}", manana);
        } catch (Exception e) {
            log.error("Error generando reservas automáticas", e);
        }
    }
}