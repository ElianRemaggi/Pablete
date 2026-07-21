package com.instituto.siga.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String dni,
        @NotBlank String password) {
}
