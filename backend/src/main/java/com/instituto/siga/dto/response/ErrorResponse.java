package com.instituto.siga.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        String codigo,
        String mensaje,
        LocalDateTime timestamp,
        List<String> detalles) {
}
