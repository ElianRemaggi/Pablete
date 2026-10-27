package com.instituto.siga.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Vista de una inscripcion a final. Los campos de alumno sirven al listado de inscriptos del
 * personal; el alumno que consulta las suyas los recibe tambien (son sus propios datos). */
public record InscripcionFinalResponse(
        Long id,
        Long mesaId,
        String materiaNombre,
        LocalDate fechaMesa,
        String turno,
        String estado,
        String profesor,
        String carreraNombre,
        String resultado,
        LocalTime hora,
        String periodo,
        String aula,
        String modalidad,
        String mesaEstado,
        LocalDateTime fechaInscripcion,
        Long alumnoId,
        String alumnoDni,
        String alumnoNombre,
        String alumnoApellido,
        String alumnoLegajo,
        Integer nota) {
}
