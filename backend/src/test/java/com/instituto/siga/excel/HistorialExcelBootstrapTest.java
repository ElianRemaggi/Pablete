package com.instituto.siga.excel;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HistorialExcelBootstrapTest {

    private final HistorialExcelBootstrap bootstrap = new HistorialExcelBootstrap();

    @Test
    void siElArchivoNoExiste_seCreaVacioDesdeLaPlantilla_yEsLegible(@TempDir Path tmp) throws IOException {
        Path destino = tmp.resolve("carpeta-nueva").resolve("historial-academico.xlsx");

        assertThat(bootstrap.asegurarArchivo(destino)).isTrue();

        assertThat(destino).exists().isRegularFile();
        assertThat(destino.resolveSibling("historial-academico.xlsx.nuevo")).doesNotExist();
        try (FileInputStream in = new FileInputStream(destino.toFile()); Workbook wb = WorkbookFactory.create(in)) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(9);
        }
        assertThat(new HistorialExcelReader().leer(destino)).isEmpty();
    }

    @Test
    void nuncaPisaUnArchivoExistente_ni_siEstaDanado(@TempDir Path tmp) throws IOException {
        Path existente = tmp.resolve("historial-academico.xlsx");
        Files.writeString(existente, "contenido danado pero que alguien debe revisar");

        assertThat(bootstrap.asegurarArchivo(existente)).isFalse();

        assertThat(Files.readString(existente)).isEqualTo("contenido danado pero que alguien debe revisar");
    }
}
