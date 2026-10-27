package com.instituto.siga.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/** Vista de una inscripcion a cursada con su acreditacion. Los campos de alumno sirven al listado de
 * inscriptos del personal; el alumno que consulta las suyas los recibe tambien (son sus propios datos).
 * resultadoCursada: PENDIENTE / APROBADA / DESAPROBADA (nunca null). */
public record InscripcionCursadaResponse(
        Long id,
        String materiaNombre,
        String comisionNombre,
        String estado,
        LocalDateTime fechaInscripcion,
        String profesor,
        String carreraNombre,
        List<HorarioResponse> horarios,
        Long materiaId,
        Long comisionId,
        String periodo,
        Long alumnoId,
        String alumnoDni,
        String alumnoNombre,
        String alumnoApellido,
        String alumnoLegajo,
        Integer notaCursada,
        String resultadoCursada,
        LocalDateTime fechaCalificacion) {
}
