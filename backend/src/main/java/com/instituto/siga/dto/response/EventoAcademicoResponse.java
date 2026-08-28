package com.instituto.siga.dto.response;

import java.time.LocalDateTime;

public record EventoAcademicoResponse(
        Long id,
        String titulo,
        String descripcion,
        String tipo,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin) {
}
