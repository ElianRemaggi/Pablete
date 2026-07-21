package com.instituto.siga.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MateriaRequest(
        @NotBlank String codigo,
        @NotBlank String nombre,
        @Min(1) int cargaHoraria) {
}
