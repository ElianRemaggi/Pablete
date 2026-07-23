package com.instituto.siga.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PlanEstudioRequest(
        @NotBlank String version,
        Boolean vigente) {
}
