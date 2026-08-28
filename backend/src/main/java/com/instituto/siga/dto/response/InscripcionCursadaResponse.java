package com.instituto.siga.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record InscripcionCursadaResponse(
        Long id,
        String materiaNombre,
        String comisionNombre,
        String estado,
        LocalDateTime fechaInscripcion,
        String profesor,
        String carreraNombre,
        List<HorarioResponse> horarios) {
}
