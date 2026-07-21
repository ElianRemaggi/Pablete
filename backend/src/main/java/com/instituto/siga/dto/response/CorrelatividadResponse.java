package com.instituto.siga.dto.response;

public record CorrelatividadResponse(
        Long id,
        Long materiaId,
        String materiaNombre,
        Long materiaRequeridaId,
        String materiaRequeridaNombre,
        String tipoRequisito) {
}
