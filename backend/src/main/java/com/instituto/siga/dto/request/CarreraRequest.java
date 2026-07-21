package com.instituto.siga.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CarreraRequest(
        @NotBlank String codigo,
        @NotBlank String nombre) {
}
