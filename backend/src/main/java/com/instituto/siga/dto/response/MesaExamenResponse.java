package com.instituto.siga.dto.response;

import java.time.LocalDate;

public record MesaExamenResponse(
        Long id,
        Long materiaId,
        String materiaNombre,
        LocalDate fecha,
        String turno,
        int cupoMaximo,
        String profesor) {
}
