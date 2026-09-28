package com.instituto.siga.dto.request;

import com.instituto.siga.entity.EstadoMateria;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EstadoAcademicoRequest(
        @NotNull EstadoMateria estado,
        @Min(1) @Max(10) Integer nota) {
}
