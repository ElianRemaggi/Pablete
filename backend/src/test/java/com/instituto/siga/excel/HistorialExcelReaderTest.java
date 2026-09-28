package com.instituto.siga.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El archivo real del historial academico tiene una estructura de encabezado poco intuitiva:
 * la fila 1 (indice 0) trae las etiquetas de materia ("N - MATERIA CURSADA/FECHA/FINAL"), pero
 * el rotulo "DNI" para la columna A esta en la fila 2 (indice 1) — y esa misma fila 2 ya tiene
 * datos de ejemplo (no encabezados) en el resto de las columnas. Este test fija ese contrato:
 * un bug real durante el desarrollo leyo los encabezados de materia desde la fila 2 y no
 * encontro ninguna, dejando el import silenciosamente vacio para casi todo el archivo real.
 */
class HistorialExcelReaderTest {

    private final HistorialExcelReader reader = new HistorialExcelReader();

    @Test
    void leer_conLaEstructuraRealDelHistorial_parseaSoloLasFilasDeAlumnos(@TempDir Path tempDir) throws IOException {
        Path archivo = tempDir.resolve("historial.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("CARRERA_TEST");

            Row fila1 = sheet.createRow(0);
            texto(fila1, 0, " ");
            texto(fila1, 1, "APELLIDO/S");
            texto(fila1, 2, "NOMBRE/S");
            texto(fila1, 3, "COHORTE");
            texto(fila1, 4, "1 - MTM 1 CURSADA");
            texto(fila1, 5, "1 - MTM 1 FECHA");
            texto(fila1, 6, "1 - MTM 1 FINAL");

            // Fila "trampa": rotulo DNI en A, pero datos de ejemplo (no encabezados) en el resto.
            Row fila2 = sheet.createRow(1);
            texto(fila2, 0, "DNI");
            texto(fila2, 1, "APELLIDO/S");
            texto(fila2, 2, "NOMBRE/S");
            numero(fila2, 3, 1);
            texto(fila2, 4, "APROBADA");
            numero(fila2, 6, 8);

            sheet.createRow(2); // fila intercalada vacia (formulas de PROMEDIO en el archivo real)

            Row fila4 = sheet.createRow(3);
            texto(fila4, 0, "12345678");
            texto(fila4, 1, "Perez");
            texto(fila4, 2, "Juan");
            numero(fila4, 3, 2020);
            texto(fila4, 4, "APROBADA");
            fila4.createCell(5).setCellValue(LocalDate.of(2024, 3, 15));
            numero(fila4, 6, 9);

            sheet.createRow(4); // otra fila vacia despues del ultimo alumno

            try (FileOutputStream out = new FileOutputStream(archivo.toFile())) {
                workbook.write(out);
            }
        }

        List<HistorialExcelRow> filas = reader.leer(archivo);

        assertThat(filas).hasSize(1);
        HistorialExcelRow fila = filas.get(0);
        assertThat(fila.dni()).isEqualTo("12345678");
        assertThat(fila.apellido()).isEqualTo("Perez");
        assertThat(fila.nombre()).isEqualTo("Juan");
        assertThat(fila.materias()).hasSize(1);
        HistorialExcelMateriaCelda celda = fila.materias().get(0);
        assertThat(celda.nombreMateria()).isEqualTo("MTM 1");
        assertThat(celda.estadoTexto()).isEqualTo("APROBADA");
        assertThat(celda.nota()).isEqualTo(9);
        assertThat(celda.fecha()).isEqualTo(LocalDate.of(2024, 3, 15));
    }

    private void texto(Row row, int columna, String valor) {
        row.createCell(columna, CellType.STRING).setCellValue(valor);
    }

    private void numero(Row row, int columna, double valor) {
        Cell celda = row.createCell(columna, CellType.NUMERIC);
        celda.setCellValue(valor);
    }
}
