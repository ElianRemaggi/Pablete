package com.instituto.siga.dto.response;

import java.time.LocalDateTime;

public record InscripcionCursadaResponse(
        Long id,
        String materiaNombre,
        String comisionNombre,
        String estado,
        LocalDateTime fechaInscripcion) {
}
