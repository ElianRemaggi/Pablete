package com.instituto.siga.dto.request;

import com.instituto.siga.entity.ResultadoFinal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Nota (1-10) obligatoria para APROBADO/DESAPROBADO; AUSENTE no lleva nota (se valida en el service). */
public record ResultadoFinalRequest(
        @NotNull ResultadoFinal resultado,
        @Min(1) @Max(10) Integer nota) {
}
