package com.instituto.siga.exportacion;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelReportBuilderTest {

    @Test
    void build_generaUnXlsxValidoConEncabezadoYFilas() throws IOException {
        byte[] xlsx = new ExcelReportBuilder("Hoja")
                .encabezado("Legajo", "Nombre")
                .fila("LEG-0001", "Juan")
                .fila("LEG-0002", "Ana")
                .build();

        assertThat(xlsx).isNotEmpty();
        assertThat(xlsx[0]).isEqualTo((byte) 'P');
        assertThat(xlsx[1]).isEqualTo((byte) 'K');

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row encabezado = sheet.getRow(0);
            assertThat(encabezado.getCell(0).getStringCellValue()).isEqualTo("Legajo");
            assertThat(encabezado.getCell(1).getStringCellValue()).isEqualTo("Nombre");
            assertThat(workbook.getFontAt(encabezado.getCell(0).getCellStyle().getFontIndexAsInt()).getBold()).isTrue();

            Row fila1 = sheet.getRow(1);
            assertThat(fila1.getCell(0).getStringCellValue()).isEqualTo("LEG-0001");
            assertThat(fila1.getCell(1).getStringCellValue()).isEqualTo("Juan");

            Row fila2 = sheet.getRow(2);
            assertThat(fila2.getCell(0).getStringCellValue()).isEqualTo("LEG-0002");
        }
    }
}
