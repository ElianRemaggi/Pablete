package com.instituto.siga.dto.response;

import java.time.LocalDateTime;

public record EstadoAcademicoResponse(
        Long alumnoId,
        Long materiaId,
        String materiaNombre,
        String estado,
        LocalDateTime fechaActualizacion) {
}
