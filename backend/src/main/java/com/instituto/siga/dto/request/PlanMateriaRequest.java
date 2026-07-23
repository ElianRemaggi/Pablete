package com.instituto.siga.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PlanMateriaRequest(
        @Min(1) int anio,
        @Min(1) @Max(2) int cuatrimestre) {
}
