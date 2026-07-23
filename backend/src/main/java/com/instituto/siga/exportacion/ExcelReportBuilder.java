package com.instituto.siga.exportacion;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Builder (docs/01 SS9): encabezado en negrita + filas de datos + autosize antes de emitir los bytes. */
public class ExcelReportBuilder {

    private final Workbook workbook = new XSSFWorkbook();
    private final Sheet sheet;
    private int filaActual = 0;
    private int columnas = 0;

    public ExcelReportBuilder(String nombreHoja) {
        this.sheet = workbook.createSheet(nombreHoja);
    }

    public ExcelReportBuilder encabezado(String... titulos) {
        CellStyle negrita = workbook.createCellStyle();
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        negrita.setFont(fuente);

        Row fila = sheet.createRow(filaActual++);
        for (int i = 0; i < titulos.length; i++) {
            Cell celda = fila.createCell(i);
            celda.setCellValue(titulos[i]);
            celda.setCellStyle(negrita);
        }
        columnas = titulos.length;
        return this;
    }

    public ExcelReportBuilder fila(Object... valores) {
        Row fila = sheet.createRow(filaActual++);
        for (int i = 0; i < valores.length; i++) {
            Cell celda = fila.createCell(i);
            Object valor = valores[i];
            if (valor instanceof Number numero) {
                celda.setCellValue(numero.doubleValue());
            } else if (valor != null) {
                celda.setCellValue(valor.toString());
            }
        }
        return this;
    }

    public byte[] build() {
        for (int i = 0; i < columnas; i++) {
            sheet.autoSizeColumn(i);
        }
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            workbook.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el archivo Excel", e);
        } finally {
            try {
                workbook.close();
            } catch (IOException ignored) {
                // el workbook ya fue escrito a memoria; un error al cerrar no invalida el resultado.
            }
        }
    }
}
