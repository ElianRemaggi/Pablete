package com.instituto.siga.dto.request;

import jakarta.validation.constraints.NotNull;

public record InscripcionCursadaRequest(
        @NotNull Long comisionId) {
}
