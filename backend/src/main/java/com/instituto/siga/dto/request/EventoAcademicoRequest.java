package com.instituto.siga.dto.request;

import com.instituto.siga.entity.TipoEventoAcademico;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record EventoAcademicoRequest(
        @NotBlank @Size(max = 150) String titulo,
        @Size(max = 500) String descripcion,
        @NotNull TipoEventoAcademico tipo,
        @NotNull LocalDateTime fechaInicio,
        LocalDateTime fechaFin) {
}
