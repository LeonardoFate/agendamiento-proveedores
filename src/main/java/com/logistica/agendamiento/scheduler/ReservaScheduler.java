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
    @Scheduled(cron = "0 0 23 * * *") // 23:00 todos los días
    public void generarReservasDelDiaSiguiente() {
        try {
            LocalDate manana = LocalDate.now().plusDays(1);
            plantillaHorarioService.generarReservasDesdeePlantillas(manana);
            log.info("Reservas automáticas generadas para {}", manana);
        } catch (Exception e) {
            log.error("Error generando reservas automáticas", e);
        }
    }

    // Generar reservas para toda la semana siguiente cada domingo a las 22:00
    @Scheduled(cron = "0 0 22 * * SUN") // 22:00 todos los domingos
    public void generarReservasSemanaCompleta() {
        try {
            LocalDate lunesProximo = LocalDate.now().plusDays(1);
            while (!lunesProximo.getDayOfWeek().toString().equals("MONDAY")) {
                lunesProximo = lunesProximo.plusDays(1);
            }

            plantillaHorarioService.generarReservasSemanaCompleta(lunesProximo);
            log.info("Reservas automáticas generadas para la semana del {}", lunesProximo);
        } catch (Exception e) {
            log.error("Error generando reservas de la semana", e);
        }
    }
}