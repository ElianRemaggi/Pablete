package com.instituto.siga.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record HistorialImportResultResponse(
        int hojasProcesadas,
        int filasLeidas,
        int actualizados,
        int alumnosNoEncontrados,
        int materiasNoEncontradas,
        LocalDateTime fecha,
        List<String> advertencias) {
}
