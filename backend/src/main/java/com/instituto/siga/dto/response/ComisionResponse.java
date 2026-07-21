package com.instituto.siga.dto.response;

import java.util.List;

public record ComisionResponse(
        Long id,
        Long materiaId,
        String materiaNombre,
        String nombre,
        String periodoLectivo,
        int cupoMaximo,
        List<HorarioResponse> horarios) {
}
