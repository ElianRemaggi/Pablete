package com.instituto.siga.excel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Parsea el .xlsx del historial academico a objetos del dominio de esta capa (HistorialExcelRow).
 * No conoce entidades JPA: el mapeo a Alumno/Materia lo hace HistorialExcelSyncService. */
@Component
public class HistorialExcelReader {

    public List<HistorialExcelRow> leer(Path archivo) throws IOException {
        HistorialExcelLock.LOCK.lock();
        try {
            return leerSinLock(archivo);
        } finally {
            HistorialExcelLock.LOCK.unlock();
        }
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

    private List<HistorialExcelRow> leerSinLock(Path archivo) throws IOException {
        List<HistorialExcelRow> filas = new ArrayList<>();
        try (FileInputStream entrada = new FileInputStream(archivo.toFile());
             Workbook workbook = WorkbookFactory.create(entrada)) {

            for (Sheet sheet : workbook) {
                HistorialExcelLayout layout = HistorialExcelLayout.leer(sheet);
                if (layout.getColumnaDni() < 0) {
                    continue; // hoja sin la estructura esperada (sin columna DNI): se ignora
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
        return filas;
    }

    private List<HistorialExcelMateriaCelda> materiasDeLaFila(Row row, HistorialExcelLayout layout) {
        List<HistorialExcelMateriaCelda> materias = new ArrayList<>();
        for (Map.Entry<String, HistorialExcelLayout.ColumnasMateria> entrada : layout.materias()) {
            HistorialExcelLayout.ColumnasMateria columnas = entrada.getValue();
            String estadoTexto = HistorialExcelCellUtils.texto(row.getCell(columnas.colCursada()));
            java.time.LocalDate fecha = HistorialExcelCellUtils.fecha(row.getCell(columnas.colFecha()));
            Integer nota = HistorialExcelCellUtils.nota(row.getCell(columnas.colFinal()));
            if (estadoTexto.isBlank() && nota == null) {
                continue; // materia sin datos para este alumno: nada que importar
            }
            materias.add(new HistorialExcelMateriaCelda(columnas.nombreOriginal(), estadoTexto, fecha, nota));
        }
        return materias;
    }
}
