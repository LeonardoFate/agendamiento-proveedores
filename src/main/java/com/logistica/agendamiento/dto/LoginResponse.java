package com.logistica.agendamiento.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tipo;
    private Long id;
    private String username;
    private String email;
    private String rol;
}