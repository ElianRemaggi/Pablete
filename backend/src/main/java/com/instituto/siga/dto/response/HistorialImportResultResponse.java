package com.instituto.siga.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado de una sincronizacion Excel -> base de datos. {@code conflictos} cuenta los registros en que
 * el Excel contradecia un dato academico ya aprobado en la base: se conserva el de la base y se informa
 * (ver HistorialExcelSyncService). {@code archivo} es la ruta efectiva que se leyo.
 */
public record HistorialImportResultResponse(
        int hojasProcesadas,
        int filasLeidas,
        int actualizados,
        int alumnosNoEncontrados,
        int materiasNoEncontradas,
        LocalDateTime fecha,
        List<String> advertencias,
        int conflictos,
        String archivo) {
}
