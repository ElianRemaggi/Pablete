package com.instituto.siga.dto.request;

import com.instituto.siga.entity.TipoRequisito;
import jakarta.validation.constraints.NotNull;

public record CorrelatividadRequest(
        @NotNull Long materiaId,
        @NotNull Long materiaRequeridaId,
        @NotNull TipoRequisito tipoRequisito) {
}
