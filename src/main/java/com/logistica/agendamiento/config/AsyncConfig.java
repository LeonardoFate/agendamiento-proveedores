package com.logistica.agendamiento.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Configuración adicional para hilos asíncronos si es necesario
}