package com.instituto.siga.excel;

import lombok.Getter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resuelve donde esta cada dato dentro de una hoja del historial academico. El encabezado real
 * (APELLIDO/S, NOMBRE/S, COHORTE y, por cada materia del plan, tres columnas consecutivas
 * "N - MATERIA CURSADA" / "N - MATERIA FECHA" / "N - MATERIA FINAL") esta en la fila 1 (indice 0).
 * La columna DNI es una excepcion: en la fila 1 esa celda esta en blanco, y la etiqueta "DNI"
 * aparece recien en la fila 2 (indice 1) — el resto de la fila 2 no es encabezado (tiene datos
 * de un alumno de ejemplo, no etiquetas), asi que solo se usa para completar DNI si hace falta.
 * Los datos reales empiezan en la fila 3 (indice 2; en el archivo original la hoja SIST_R6790_19 ya
 * tiene alumnos ahi, y en las demas esa fila esta vacia); las filas intercaladas vacias (con formulas
 * de PROMEDIO que devuelven 0 sin alumno) se descartan por no tener DNI, no por posicion fija.
 */
@Getter
public final class HistorialExcelLayout {

    static final int FILA_ENCABEZADO = 0;
    static final int FILA_ENCABEZADO_DNI_FALLBACK = 1;
    static final int PRIMERA_FILA_DATOS = 2;

    private int columnaDni = -1;
    private int columnaApellido = -1;
    private int columnaNombre = -1;
    private int columnaCohorte = -1;
    private final Map<String, ColumnasMateria> columnasPorMateria = new LinkedHashMap<>();
    /** Problemas de estructura detectados al leer el encabezado (materia repetida, columnas FECHA/FINAL ausentes). */
    private final List<String> problemasDeEstructura = new ArrayList<>();

    private HistorialExcelLayout() {
    }

    record ColumnasMateria(int colCursada, int colFecha, int colFinal, String nombreOriginal) {
    }

    static HistorialExcelLayout leer(Sheet sheet) {
        HistorialExcelLayout layout = new HistorialExcelLayout();
        Row encabezado = sheet.getRow(FILA_ENCABEZADO);
        if (encabezado == null) {
            return layout;
        }

        Map<Integer, String> encabezadosPorColumna = new LinkedHashMap<>();
        for (Cell celda : encabezado) {
            String texto = HistorialExcelCellUtils.texto(celda);
            if (texto.isBlank()) continue;
            encabezadosPorColumna.put(celda.getColumnIndex(), texto);
            switch (texto.toUpperCase(Locale.ROOT)) {
                case "DNI" -> layout.columnaDni = celda.getColumnIndex();
                case "APELLIDO/S" -> layout.columnaApellido = celda.getColumnIndex();
                case "NOMBRE/S" -> layout.columnaNombre = celda.getColumnIndex();
                case "COHORTE" -> layout.columnaCohorte = celda.getColumnIndex();
                default -> { /* columna de materia, se procesa abajo */ }
            }
        }

        if (layout.columnaDni < 0) {
            layout.columnaDni = buscarDniEnFilaSiguiente(sheet);
        }

        for (Map.Entry<Integer, String> entrada : encabezadosPorColumna.entrySet()) {
            String texto = entrada.getValue();
            if (!texto.toUpperCase(Locale.ROOT).endsWith(" CURSADA")) continue;

            int colCursada = entrada.getKey();
            String nombreOriginal = HistorialExcelCellUtils.normalizarNombreMateria(
                    texto.substring(0, texto.length() - " CURSADA".length()));

            // El archivo original tiene un encabezado con el sufijo en plural ("15 - SO FECHAS"): se tolera.
            Integer colFecha = columnaConSufijo(encabezadosPorColumna, colCursada + 1, " FECHA", " FECHAS");
            Integer colFinal = columnaConSufijo(encabezadosPorColumna, colCursada + 2, " FINAL");
            if (colFecha == null || colFinal == null) {
                // estructura inesperada para esta columna: se ignora en vez de romper el import, pero se informa
                layout.problemasDeEstructura.add("Hoja " + sheet.getSheetName() + ": \"" + texto
                        + "\" no tiene las columnas FECHA y FINAL a continuación; esa materia no se sincroniza");
                continue;
            }
            String clave = HistorialExcelCellUtils.claveComparacion(nombreOriginal);
            if (layout.columnasPorMateria.containsKey(clave)) {
                // Dos columnas con el mismo nombre (p. ej. \"EDI\" 3 veces en PROG_R6175_03) no se pueden distinguir
                // por nombre: se usa la primera y se informa, en vez de pisarla en silencio.
                layout.problemasDeEstructura.add("Hoja " + sheet.getSheetName() + ": la materia \"" + nombreOriginal
                        + "\" figura en más de una columna (\"" + texto + "\"); solo se sincroniza la primera");
                continue;
            }
            layout.columnasPorMateria.put(clave, new ColumnasMateria(colCursada, colFecha, colFinal, nombreOriginal));
        }
        return layout;
    }

    private static Integer columnaConSufijo(Map<Integer, String> encabezados, int columna, String... sufijos) {
        String texto = encabezados.get(columna);
        if (texto == null) return null;
        String mayusculas = texto.toUpperCase(Locale.ROOT);
        for (String sufijo : sufijos) {
            if (mayusculas.endsWith(sufijo)) return columna;
        }
        return null;
    }

    /** Fallback: busca una celda con el texto exacto "DNI" en la fila 2, solo para resolver la
     * columna cuando la fila 1 la tiene en blanco (ver comentario de clase). No trata el resto
     * de esa fila como encabezado. */
    private static int buscarDniEnFilaSiguiente(Sheet sheet) {
        Row fila = sheet.getRow(FILA_ENCABEZADO_DNI_FALLBACK);
        if (fila == null) return -1;
        for (Cell celda : fila) {
            if ("DNI".equals(HistorialExcelCellUtils.texto(celda).toUpperCase(Locale.ROOT))) {
                return celda.getColumnIndex();
            }
        }
        return -1;
    }

    ColumnasMateria columnas(String nombreMateria) {
        return columnasPorMateria.get(HistorialExcelCellUtils.claveComparacion(nombreMateria));
    }

    Iterable<Map.Entry<String, ColumnasMateria>> materias() {
        return columnasPorMateria.entrySet();
    }
}
