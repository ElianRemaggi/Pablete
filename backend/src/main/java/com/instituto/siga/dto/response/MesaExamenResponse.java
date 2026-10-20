package com.instituto.siga.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record MesaExamenResponse(
        Long id,
        Long materiaId,
        String materiaNombre,
        LocalDate fecha,
        String turno,
        int cupoMaximo,
        String profesor,
        LocalTime hora,
        String periodo,
        String aula,
        String modalidad,
        LocalDateTime aperturaInscripcion,
        LocalDateTime cierreInscripcion,
        String estado,
        long inscriptos,
        long cupoDisponible,
        boolean inscripcionAbierta) {
}
