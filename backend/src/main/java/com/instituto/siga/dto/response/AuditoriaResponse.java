package com.instituto.siga.dto.response;

import java.time.LocalDateTime;

public record AuditoriaResponse(
        Long id,
        Long usuarioId,
        String usuarioNombre,
        String accion,
        String entidad,
        Long entidadId,
        String detalle,
        String ip,
        LocalDateTime fecha) {
}
