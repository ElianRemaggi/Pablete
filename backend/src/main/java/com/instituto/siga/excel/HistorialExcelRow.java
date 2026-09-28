package com.instituto.siga.excel;

import java.util.List;

/** Una fila de alumno leida de una hoja del historial academico (una hoja = una carrera/plan). */
public record HistorialExcelRow(
        String hoja,
        String dni,
        String apellido,
        String nombre,
        String cohorte,
        List<HistorialExcelMateriaCelda> materias) {
}
