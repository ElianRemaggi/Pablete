package com.instituto.siga.dto.response;

import java.time.LocalDate;

public record InscripcionFinalResponse(
        Long id,
        String materiaNombre,
        LocalDate fechaMesa,
        String turno,
        String estado,
        String profesor,
        String carreraNombre,
        String resultado) {
}
