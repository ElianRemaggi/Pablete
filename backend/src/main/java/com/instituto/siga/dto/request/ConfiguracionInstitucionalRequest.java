package com.instituto.siga.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ConfiguracionInstitucionalRequest(
        @NotNull LocalDateTime fechaAperturaCursadas,
        @NotNull LocalDateTime fechaCierreCursadas,
        @NotNull LocalDateTime fechaAperturaFinales,
        @NotNull LocalDateTime fechaCierreFinales,
        @Min(1) int limiteIntentosFinal,
        @Min(1) int horasBloqueoFinal) {
}
