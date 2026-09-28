package com.instituto.siga.excel;

import java.time.LocalDate;

/** Una materia del plan para un alumno: lo que dice la hoja en las columnas CURSADA/FECHA/FINAL. */
public record HistorialExcelMateriaCelda(
        String nombreMateria,
        String estadoTexto,
        LocalDate fecha,
        Integer nota) {
}
