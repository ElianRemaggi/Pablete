package com.instituto.siga.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Parsea el .xlsx del historial academico a objetos del dominio de esta capa (HistorialExcelRow).
 * No conoce entidades JPA: el mapeo a Alumno/Materia lo hace HistorialExcelSyncService (archivo en disco) o
 * HistorialExcelImportService (archivo subido desde la interfaz). */
@Component
public class HistorialExcelReader {

    /** Hojas y filas leidas de un libro, mas los problemas de estructura de sus encabezados. */
    public record Lectura(List<HistorialExcelRow> filas, List<String> problemasDeEstructura) {
    }

    public List<HistorialExcelRow> leer(Path archivo) throws IOException {
        HistorialExcelLock.LOCK.lock();
        try (FileInputStream entrada = new FileInputStream(archivo.toFile())) {
            return leerLibro(entrada).filas();
        } finally {
            HistorialExcelLock.LOCK.unlock();
        }
    }

    /** Lectura de un archivo subido (no toca el .xlsx configurado en disco, por eso no usa el candado de escritura). */
    public Lectura leer(InputStream entrada) throws IOException {
        return leerLibro(entrada);
    }

    /** Problemas de estructura de los encabezados de todas las hojas (materias repetidas, columnas faltantes). */
    public List<String> problemasDeEstructura(Path archivo) throws IOException {
        HistorialExcelLock.LOCK.lock();
        try (FileInputStream entrada = new FileInputStream(archivo.toFile());
             Workbook workbook = WorkbookFactory.create(entrada)) {
            List<String> problemas = new ArrayList<>();
            for (Sheet sheet : workbook) {
                problemas.addAll(HistorialExcelLayout.leer(sheet).getProblemasDeEstructura());
            }
            return problemas;
        } finally {
            HistorialExcelLock.LOCK.unlock();
        }
    }

    private Lectura leerLibro(InputStream entrada) throws IOException {
        List<HistorialExcelRow> filas = new ArrayList<>();
        List<String> problemas = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(entrada)) {
            for (Sheet sheet : workbook) {
                HistorialExcelLayout layout = HistorialExcelLayout.leer(sheet);
                problemas.addAll(layout.getProblemasDeEstructura());
                if (layout.getColumnaDni() < 0) {
                    problemas.add("Hoja " + sheet.getSheetName() + ": falta la columna obligatoria DNI; la hoja se ignora");
                    continue; // hoja sin la estructura esperada (sin columna DNI): se ignora
                }
                if (!layout.materias().iterator().hasNext()) {
                    problemas.add("Hoja " + sheet.getSheetName()
                            + ": no tiene columnas de materias (\"N - MATERIA CURSADA\" / \"FECHA\" / \"FINAL\")");
                }
                int ultimaFila = sheet.getLastRowNum();
                for (int i = HistorialExcelLayout.PRIMERA_FILA_DATOS; i <= ultimaFila; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    String dni = HistorialExcelCellUtils.texto(row.getCell(layout.getColumnaDni()));
                    if (dni.isBlank()) continue; // fila intercalada vacia (o fin de los datos)

                    filas.add(new HistorialExcelRow(
                            sheet.getSheetName(),
                            dni,
                            layout.getColumnaApellido() >= 0 ? HistorialExcelCellUtils.texto(row.getCell(layout.getColumnaApellido())) : "",
                            layout.getColumnaNombre() >= 0 ? HistorialExcelCellUtils.texto(row.getCell(layout.getColumnaNombre())) : "",
                            layout.getColumnaCohorte() >= 0 ? HistorialExcelCellUtils.texto(row.getCell(layout.getColumnaCohorte())) : "",
                            materiasDeLaFila(row, layout)));
                }
            }
        }
        return new Lectura(filas, problemas);
    }

    private List<HistorialExcelMateriaCelda> materiasDeLaFila(Row row, HistorialExcelLayout layout) {
        List<HistorialExcelMateriaCelda> materias = new ArrayList<>();
        for (Map.Entry<String, HistorialExcelLayout.ColumnasMateria> entrada : layout.materias()) {
            HistorialExcelLayout.ColumnasMateria columnas = entrada.getValue();
            String estadoTexto = HistorialExcelCellUtils.texto(row.getCell(columnas.colCursada()));
            java.time.LocalDate fecha = HistorialExcelCellUtils.fecha(row.getCell(columnas.colFecha()));
            Cell celdaNota = row.getCell(columnas.colFinal());
            Integer nota = HistorialExcelCellUtils.nota(celdaNota);
            String notaTexto = null;
            if (nota == null && celdaNota != null && celdaNota.getCellType() == CellType.STRING) {
                String t = HistorialExcelCellUtils.texto(celdaNota);
                if (!t.isBlank() && !t.matches("-+")) {
                    notaTexto = t;
                }
            }
            if (estadoTexto.isBlank() && nota == null && notaTexto == null) {
                continue; // materia sin datos para este alumno: nada que importar
            }
            materias.add(new HistorialExcelMateriaCelda(columnas.nombreOriginal(), estadoTexto, fecha, nota, notaTexto));
        }
        return materias;
    }
}
