package com.instituto.siga.dto.request;

import com.instituto.siga.entity.ResultadoCursada;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Nota (1-10) obligatoria para APROBADA/DESAPROBADA; PENDIENTE la deja sin nota (se valida en el service). */
public record CalificacionCursadaRequest(
        @NotNull ResultadoCursada resultado,
        @Min(1) @Max(10) Integer nota) {
}
