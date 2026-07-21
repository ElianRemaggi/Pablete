package com.instituto.siga.dto.request;

import com.instituto.siga.entity.Turno;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MesaExamenRequest(
        @NotNull Long materiaId,
        @NotNull LocalDate fecha,
        @NotNull Turno turno,
        @Min(1) int cupoMaximo) {
}
