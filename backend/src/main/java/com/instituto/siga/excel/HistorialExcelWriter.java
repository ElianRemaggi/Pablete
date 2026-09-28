package com.instituto.siga.excel;

import com.instituto.siga.entity.EstadoMateria;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Escribe un cambio de estado academico en la fila/columna correspondiente del .xlsx.
 * No crea filas ni hojas nuevas: si el alumno o la materia no estan en el archivo (porque nunca
 * se importaron desde ahi), no escribe nada — evita corromper la estructura de un plan que no
 * reconoce. Escritura atomica: arma el libro en memoria, lo vuelca a un archivo temporal, hace
 * una copia de respaldo del original y recien entonces reemplaza el archivo real.
 */
@Slf4j
@Component
public class HistorialExcelWriter {

    private final ReentrantLock lock = new ReentrantLock();

    public boolean actualizarCelda(Path archivo, String dni, String nombreMateria,
                                    EstadoMateria estado, Integer nota, LocalDate fecha) {
        if (dni == null || dni.isBlank() || nombreMateria == null || nombreMateria.isBlank()) {
            return false;
        }
        lock.lock();
        try {
            if (!Files.exists(archivo)) {
                log.warn("No se sincronizo a Excel (archivo inexistente): {}", archivo);
                return false;
            }
            try (Workbook workbook = abrir(archivo)) {
                boolean actualizado = escribirCelda(workbook, dni, nombreMateria, estado, nota, fecha);
                if (actualizado) {
                    guardarConBackup(archivo, workbook);
                } else {
                    log.info("DNI {} / materia \"{}\" no esta en el historial Excel: no se sincronizo esa celda",
                            dni, nombreMateria);
                }
                return actualizado;
            }
        } catch (IOException e) {
            log.error("No se pudo escribir el cambio en el historial Excel ({})", archivo, e);
            return false;
        } finally {
            lock.unlock();
        }
    }

    private Workbook abrir(Path archivo) throws IOException {
        try (FileInputStream entrada = new FileInputStream(archivo.toFile())) {
            return WorkbookFactory.create(entrada);
        }
    }

    private boolean escribirCelda(Workbook workbook, String dni, String nombreMateria,
                                   EstadoMateria estado, Integer nota, LocalDate fecha) {
        for (Sheet sheet : workbook) {
            HistorialExcelLayout layout = HistorialExcelLayout.leer(sheet);
            if (layout.getColumnaDni() < 0) continue;
            HistorialExcelLayout.ColumnasMateria columnas = layout.columnas(nombreMateria);
            if (columnas == null) continue;

            int ultimaFila = sheet.getLastRowNum();
            for (int i = HistorialExcelLayout.PRIMERA_FILA_DATOS; i <= ultimaFila; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String dniFila = HistorialExcelCellUtils.texto(row.getCell(layout.getColumnaDni()));
                if (!dniFila.equals(dni)) continue;

                celda(row, columnas.colCursada()).setCellValue(estado.name());
                if (fecha != null) {
                    celda(row, columnas.colFecha()).setCellValue(fecha);
                }
                if (nota != null) {
                    celda(row, columnas.colFinal()).setCellValue(nota);
                }
                return true;
            }
        }
        return false;
    }

    /** Reusa la celda existente (y su estilo/formato) en vez de reemplazarla. */
    private Cell celda(Row row, int columna) {
        Cell existente = row.getCell(columna);
        return existente != null ? existente : row.createCell(columna);
    }

    private void guardarConBackup(Path archivo, Workbook workbook) throws IOException {
        Path temporal = archivo.resolveSibling(archivo.getFileName().toString() + ".tmp");
        Path backup = archivo.resolveSibling(archivo.getFileName().toString() + ".bak");

        try (FileOutputStream salida = new FileOutputStream(temporal.toFile())) {
            workbook.write(salida);
        }
        Files.copy(archivo, backup, StandardCopyOption.REPLACE_EXISTING);
        Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
