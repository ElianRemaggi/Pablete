package com.instituto.siga.dto.response;

public record MateriaResponse(
        Long id,
        String codigo,
        String nombre,
        int cargaHoraria,
        boolean activa) {
}
