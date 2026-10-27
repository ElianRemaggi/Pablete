package com.instituto.siga.dto.response;

/** Una correlatividad de la materia evaluada contra el estado academico del alumno. */
public record RequisitoResponse(
        Long materiaRequeridaId,
        String materiaRequeridaNombre,
        String tipoRequisito,
        boolean cumplido,
        String descripcion) {
}
