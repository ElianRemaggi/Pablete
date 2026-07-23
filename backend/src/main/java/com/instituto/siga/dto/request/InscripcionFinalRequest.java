package com.instituto.siga.dto.request;

import jakarta.validation.constraints.NotNull;

public record InscripcionFinalRequest(
        @NotNull Long mesaId) {
}
