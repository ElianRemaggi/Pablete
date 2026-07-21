package com.instituto.siga.dto.response;

public record CarreraResponse(
        Long id,
        String codigo,
        String nombre,
        boolean activa) {
}
