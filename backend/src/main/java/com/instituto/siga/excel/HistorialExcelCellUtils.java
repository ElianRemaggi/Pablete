package com.instituto.siga.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lectura tolerante de celdas del historial en Excel: DNI/notas pueden venir como texto o
 * numero segun como los haya tipeado cada usuario a lo largo de los anios, y las fechas de
 * examen se guardan como serial numerico de Excel, no como texto. */
final class HistorialExcelCellUtils {

    private static final Pattern PREFIJO_ORDINAL = Pattern.compile("^\\d+\\s*-\\s*(.+)$");

    private HistorialExcelCellUtils() {
    }

    static String texto(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double valor = cell.getNumericCellValue();
                yield (valor == Math.floor(valor) && !Double.isInfinite(valor))
                        ? String.valueOf((long) valor)
                        : String.valueOf(valor);
            }
            default -> "";
        };
    }

    static LocalDate fecha(Cell cell) {
        if (cell == null || cell.getCellType() != CellType.NUMERIC) return null;
        double valor = cell.getNumericCellValue();
        if (valor <= 0) return null;
        try {
            return DateUtil.getLocalDateTime(valor).toLocalDate();
        } catch (RuntimeException e) {
            return null;
        }
    }

    static Integer nota(Cell cell) {
        if (cell == null || cell.getCellType() != CellType.NUMERIC) return null;
        return (int) Math.round(cell.getNumericCellValue());
    }

    /** "3 - Q INORGANICA" -> "Q INORGANICA" (sin el orden dentro del plan, que no forma parte
     * del nombre de la materia). Si no matchea el patron, devuelve el texto tal cual. */
    static String normalizarNombreMateria(String texto) {
        if (texto == null) return "";
        String limpio = texto.trim();
        Matcher m = PREFIJO_ORDINAL.matcher(limpio);
        return (m.matches() ? m.group(1) : limpio).trim();
    }

    static String claveComparacion(String nombreMateria) {
        return nombreMateria == null ? "" : nombreMateria.trim().toUpperCase(Locale.ROOT);
    }
}
