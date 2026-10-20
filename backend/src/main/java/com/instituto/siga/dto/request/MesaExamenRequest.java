package com.instituto.siga.dto.request;

import com.instituto.siga.entity.Modalidad;
import com.instituto.siga.entity.Turno;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Los campos desde {@code hora} en adelante son opcionales: una mesa sin ventana propia usa la
 * ventana general de ConfiguracionInstitucional. La mesa siempre se crea en BORRADOR; publicarla es
 * una accion aparte. */
public record MesaExamenRequest(
        @NotNull Long materiaId,
        @NotNull LocalDate fecha,
        @NotNull Turno turno,
        @Min(1) int cupoMaximo,
        String profesor,
        LocalTime hora,
        @Size(max = 20) String periodo,
        @Size(max = 100) String aula,
        Modalidad modalidad,
        LocalDateTime aperturaInscripcion,
        LocalDateTime cierreInscripcion) {
}
