package com.logistica.agendamiento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AgendamientoProveedoresApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgendamientoProveedoresApplication.class, args);
    }

}
