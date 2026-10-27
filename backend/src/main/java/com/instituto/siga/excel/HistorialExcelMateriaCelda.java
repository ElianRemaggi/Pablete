package com.instituto.siga.excel;

import java.time.LocalDate;

/** Una materia del plan para un alumno: lo que dice la hoja en las columnas CURSADA/FECHA/FINAL.
 * notaTexto es el contenido de la celda FINAL cuando NO es un numero (p. ej. "abc"): la nota queda null y el
 * importador lo informa como nota invalida en vez de ignorarlo en silencio. */
public record HistorialExcelMateriaCelda(
        String nombreMateria,
        String estadoTexto,
        LocalDate fecha,
        Integer nota,
        String notaTexto) {

    public HistorialExcelMateriaCelda(String nombreMateria, String estadoTexto, LocalDate fecha, Integer nota) {
        this(nombreMateria, estadoTexto, fecha, nota, null);
    }
}
