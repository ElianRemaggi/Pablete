package com.instituto.siga.dto.response;

import java.time.LocalDateTime;

public record HistorialAcademicoResponse(
        String materiaCodigo,
        String materiaNombre,
        String estado,
        LocalDateTime fechaActualizacion) {
}
