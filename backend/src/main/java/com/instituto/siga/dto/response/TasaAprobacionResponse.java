package com.instituto.siga.dto.response;

public record TasaAprobacionResponse(
        Long materiaId,
        String materiaNombre,
        long aprobadas,
        long total) {
}
