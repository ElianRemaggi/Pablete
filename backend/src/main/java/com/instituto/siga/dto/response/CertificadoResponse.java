package com.instituto.siga.dto.response;

import java.time.LocalDateTime;

public record CertificadoResponse(
        Long id,
        String tipo,
        LocalDateTime fechaEmision,
        String urlDescarga) {
}
