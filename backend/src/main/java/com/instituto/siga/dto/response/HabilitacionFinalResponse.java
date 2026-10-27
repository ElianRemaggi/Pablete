package com.instituto.siga.dto.response;

import java.util.List;

/** Si el alumno puede inscribirse al final de la materia y, si no, que le falta. */
public record HabilitacionFinalResponse(
        Long materiaId,
        String materiaNombre,
        boolean cursadaAprobada,
        boolean habilitado,
        List<RequisitoResponse> correlatividades,
        List<String> pendientes) {
}
