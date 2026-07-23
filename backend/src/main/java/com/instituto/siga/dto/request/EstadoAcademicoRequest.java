package com.instituto.siga.dto.request;

import com.instituto.siga.entity.EstadoMateria;
import jakarta.validation.constraints.NotNull;

public record EstadoAcademicoRequest(
        @NotNull EstadoMateria estado) {
}
