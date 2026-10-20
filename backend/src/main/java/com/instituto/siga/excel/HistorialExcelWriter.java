package com.instituto.siga.excel;

import com.instituto.siga.entity.EstadoMateria;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
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

/**
 * Escribe un cambio de estado academico en la fila/columna correspondiente del .xlsx.
 * No crea filas ni hojas nuevas: si el alumno o la materia no estan en el archivo (porque nunca
 * se importaron desde ahi), no escribe nada — evita corromper la estructura de un plan que no
 * reconoce — y lo informa con un {@link Resultado} concreto, no con un booleano mudo.
 * Escritura segura: arma el libro en memoria, lo vuelca a un archivo temporal, hace una copia de
 * respaldo (.bak) del original y recien entonces reemplaza el archivo real. Si algo falla (archivo
 * bloqueado por Excel, disco lleno, etc.) el original queda intacto y el temporal se descarta.
 */
@Slf4j
@Component
public class HistorialExcelWriter {

    /** Resultado de un intento de escritura; {@code mensaje} es apto para mostrar al usuario. */
    public record Resultado(Estado estado, String mensaje) {

        public enum Estado { ESCRITO, ARCHIVO_INEXISTENTE, NO_ESTA_EN_EL_ARCHIVO, ERROR }

        public boolean escrito() {
            return estado == Estado.ESCRITO;
        }

        static Resultado de(Estado estado, String mensaje) {
            return new Resultado(estado, mensaje);
        }
    }

    public Resultado actualizarCelda(Path archivo, String dni, String nombreMateria,
                                      EstadoMateria estado, Integer nota, LocalDate fecha) {
        if (dni == null || dni.isBlank() || nombreMateria == null || nombreMateria.isBlank() || estado == null) {
            return Resultado.de(Resultado.Estado.NO_ESTA_EN_EL_ARCHIVO, "Faltan datos para ubicar la celda en el Excel");
        }
        HistorialExcelLock.LOCK.lock();
        try {
            if (!Files.exists(archivo)) {
                log.warn("No se sincronizo a Excel (archivo inexistente): {}", archivo);
                return Resultado.de(Resultado.Estado.ARCHIVO_INEXISTENTE,
                        "No existe el archivo del historial: " + archivo);
            }
            try (Workbook workbook = abrir(archivo)) {
                if (!escribirCelda(workbook, dni, nombreMateria, estado, nota, fecha)) {
                    log.info("DNI {} / materia \"{}\" no esta en el historial Excel: no se sincronizo esa celda",
                            dni, nombreMateria);
                    return Resultado.de(Resultado.Estado.NO_ESTA_EN_EL_ARCHIVO,
                            "El DNI " + dni + " o la materia \"" + nombreMateria
                                    + "\" no figuran en el Excel del historial; el cambio quedó solo en la base de datos");
                }
                guardarConBackup(archivo, workbook);
                return Resultado.de(Resultado.Estado.ESCRITO, "Cambio guardado en el Excel del historial");
            }
        } catch (IOException | RuntimeException e) {
            log.error("No se pudo escribir el cambio en el historial Excel ({})", archivo, e);
            return Resultado.de(Resultado.Estado.ERROR, "No se pudo escribir en el Excel del historial ("
                    + e.getClass().getSimpleName() + ": " + e.getMessage()
                    + "). ¿Está abierto en otro programa? El cambio quedó solo en la base de datos");
        } finally {
            HistorialExcelLock.LOCK.unlock();
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
                    Cell celdaFecha = celda(row, columnas.colFecha());
                    celdaFecha.setCellValue(fecha);
                    if (!DateUtil.isCellDateFormatted(celdaFecha)) {
                        celdaFecha.setCellStyle(estiloFecha(workbook, celdaFecha));
                    }
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

    /** Una celda vacia no trae formato de fecha: sin esto el serial numerico se veria como "45123". Se
     * conserva el resto del estilo de la celda y solo se fuerza el formato dd-mm-yy del archivo original. */
    private CellStyle estiloFecha(Workbook workbook, Cell celda) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.cloneStyleFrom(celda.getCellStyle());
        estilo.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd-mm-yy"));
        return estilo;
    }

    private void guardarConBackup(Path archivo, Workbook workbook) throws IOException {
        Path temporal = archivo.resolveSibling(archivo.getFileName().toString() + ".tmp");
        Path backup = archivo.resolveSibling(archivo.getFileName().toString() + ".bak");
        try {
            try (FileOutputStream salida = new FileOutputStream(temporal.toFile())) {
                workbook.write(salida);
            }
            Files.copy(archivo, backup, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporal); // no deja basura si el reemplazo fallo (p. ej. archivo bloqueado)
        }
    }
}
