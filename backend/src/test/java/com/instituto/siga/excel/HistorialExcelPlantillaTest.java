package com.instituto.siga.excel;

import com.instituto.siga.entity.EstadoMateria;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el archivo real {@code data/historial-academico.xlsx} (solo lectura: nunca se escribe sobre el)
 * y la escritura/lectura sobre COPIAS temporales con un alumno de prueba identificable (DNI 99000001,
 * apellido TEST-EXCEL). El archivo real no se modifica en ningun momento.
 */
class HistorialExcelPlantillaTest {

    private static final String DNI_PRUEBA = "99000001";

    private final HistorialExcelReader reader = new HistorialExcelReader();
    private final HistorialExcelWriter writer = new HistorialExcelWriter();

    private Path plantilla;

    @BeforeEach
    void ubicarPlantilla() {
        plantilla = new HistorialExcelPath("./data/historial-academico.xlsx").resolver();
    }

    /** Hoja y columnas de la primera materia de la primera hoja que tiene la estructura esperada. */
    private record Destino(String hoja, int indiceHoja, String materia) {
    }

    private Destino primerDestino(Path archivo) throws IOException {
        try (FileInputStream in = new FileInputStream(archivo.toFile()); Workbook wb = WorkbookFactory.create(in)) {
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                HistorialExcelLayout layout = HistorialExcelLayout.leer(wb.getSheetAt(i));
                if (layout.getColumnaDni() >= 0 && layout.materias().iterator().hasNext()) {
                    String materia = layout.materias().iterator().next().getValue().nombreOriginal();
                    return new Destino(wb.getSheetName(i), i, materia);
                }
            }
        }
        throw new AssertionError("ninguna hoja con estructura de historial");
    }

    /** Agrega al final de la hoja un alumno de prueba, con las celdas de materia vacias. */
    private void agregarAlumnoDePrueba(Path archivo, Destino destino) throws IOException {
        try (FileInputStream in = new FileInputStream(archivo.toFile()); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(destino.indiceHoja());
            HistorialExcelLayout layout = HistorialExcelLayout.leer(sheet);
            Row fila = sheet.createRow(Math.max(sheet.getLastRowNum() + 1, HistorialExcelLayout.PRIMERA_FILA_DATOS));
            fila.createCell(layout.getColumnaDni()).setCellValue(Double.parseDouble(DNI_PRUEBA));
            fila.createCell(layout.getColumnaApellido()).setCellValue("TEST-EXCEL");
            fila.createCell(layout.getColumnaNombre()).setCellValue("Alumno");
            fila.createCell(layout.getColumnaCohorte()).setCellValue(2026);
            try (FileOutputStream out = new FileOutputStream(archivo.toFile())) {
                wb.write(out);
            }
        }
    }

    // ---------------------------------------------------------------- archivo real

    @Test
    void elArchivoRealExiste_esUnXlsxValido_yTieneLaEstructuraOriginal() throws IOException {
        assertThat(plantilla).as("ubicacion efectiva del historial").exists().isRegularFile();
        byte[] cabecera = Files.readAllBytes(plantilla);
        assertThat(new String(cabecera, 0, 2, StandardCharsets.ISO_8859_1)).as("firma ZIP de un .xlsx real, no CSV/texto").isEqualTo("PK");

        List<String> hojas = new ArrayList<>();
        try (FileInputStream in = new FileInputStream(plantilla.toFile()); Workbook wb = WorkbookFactory.create(in)) {
            for (Sheet sheet : wb) {
                hojas.add(sheet.getSheetName());
                HistorialExcelLayout layout = HistorialExcelLayout.leer(sheet);
                assertThat(layout.getColumnaDni()).as("columna DNI en %s", sheet.getSheetName()).isGreaterThanOrEqualTo(0);
                assertThat(layout.getColumnaApellido()).as("APELLIDO/S en %s", sheet.getSheetName()).isGreaterThanOrEqualTo(0);
                assertThat(layout.getColumnaNombre()).as("NOMBRE/S en %s", sheet.getSheetName()).isGreaterThanOrEqualTo(0);
                assertThat(layout.getColumnaCohorte()).as("COHORTE en %s", sheet.getSheetName()).isGreaterThanOrEqualTo(0);
                assertThat(layout.getColumnasPorMateria()).as("materias en %s", sheet.getSheetName()).isNotEmpty();
            }
        }
        // Las 9 hojas (una por carrera/plan) del ejemplo original
        assertThat(hojas).containsExactly("ALIM_R11297_97", "PROG_R6175_03", "ENF_R854_16", "EMER_R42_17", "LAC_R205_18",
                "IQUI_R5620_19", "SIST_R6790_19", "CDIA_R2730_22", "ALIM_R5563_23");
    }

    @Test
    void elArchivoRealNoContieneDatosDeAlumnos_niCredenciales() throws IOException {
        assertThat(reader.leer(plantilla)).as("filas de alumnos en el archivo real").isEmpty();

        try (FileInputStream in = new FileInputStream(plantilla.toFile()); Workbook wb = WorkbookFactory.create(in)) {
            for (Sheet sheet : wb) {
                for (Row row : sheet) {
                    if (row.getRowNum() < 2) continue;
                    for (Cell cell : row) {
                        assertThat(HistorialExcelCellUtils.texto(cell)).as("celda %s!%s", sheet.getSheetName(), cell.getAddress()).isBlank();
                    }
                }
            }
        }
        String contenido = new String(Files.readAllBytes(plantilla), StandardCharsets.ISO_8859_1).toLowerCase();
        assertThat(contenido).doesNotContain("password").doesNotContain("token");
    }

    // ----------------------------------------------------- copia temporal: leer/escribir/releer

    @Test
    void registroDePrueba_seLee_seEscribeConElWriter_yElArchivoSeReabre(@TempDir Path tmp) throws IOException {
        Path copia = tmp.resolve("historial-copia.xlsx");
        Files.copy(plantilla, copia, StandardCopyOption.REPLACE_EXISTING);
        Destino destino = primerDestino(copia);
        agregarAlumnoDePrueba(copia, destino);

        // 1. El reader lee el registro controlado
        List<HistorialExcelRow> antes = reader.leer(copia);
        assertThat(antes).hasSize(1);
        assertThat(antes.get(0).dni()).isEqualTo(DNI_PRUEBA);
        assertThat(antes.get(0).apellido()).isEqualTo("TEST-EXCEL");
        assertThat(antes.get(0).hoja()).isEqualTo(destino.hoja());
        assertThat(antes.get(0).materias()).isEmpty();

        // 2. El writer escribe estado, nota y fecha
        LocalDate fecha = LocalDate.of(2026, 3, 10);
        HistorialExcelWriter.Resultado resultado = writer.actualizarCelda(copia, DNI_PRUEBA, destino.materia(),
                EstadoMateria.APROBADA, 8, fecha);
        assertThat(resultado.escrito()).as(resultado.mensaje()).isTrue();

        // 3. Se relee: persistio fisicamente y el archivo sigue siendo un xlsx valido
        List<HistorialExcelRow> despues = reader.leer(copia);
        assertThat(despues).hasSize(1);
        HistorialExcelMateriaCelda celda = despues.get(0).materias().get(0);
        assertThat(celda.nombreMateria()).isEqualTo(destino.materia());
        assertThat(celda.estadoTexto()).isEqualTo("APROBADA");
        assertThat(celda.nota()).isEqualTo(8);
        assertThat(celda.fecha()).isEqualTo(fecha);

        // 4. La fecha se guardo con formato de fecha (no como serial numerico "46091")
        try (FileInputStream in = new FileInputStream(copia.toFile()); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(destino.indiceHoja());
            HistorialExcelLayout layout = HistorialExcelLayout.leer(sheet);
            Row fila = sheet.getRow(sheet.getLastRowNum());
            Cell celdaFecha = fila.getCell(layout.columnas(destino.materia()).colFecha());
            assertThat(DateUtil.isCellDateFormatted(celdaFecha)).isTrue();
        }

        // 5. Respaldo creado, sin temporales sueltos, y el original (plantilla) intacto
        assertThat(copia.resolveSibling("historial-copia.xlsx.bak")).exists();
        assertThat(copia.resolveSibling("historial-copia.xlsx.tmp")).doesNotExist();
        assertThat(reader.leer(plantilla)).isEmpty();
    }

    @Test
    void escribirDosVecesLoMismo_noDuplicaFilas(@TempDir Path tmp) throws IOException {
        Path copia = tmp.resolve("historial-copia.xlsx");
        Files.copy(plantilla, copia);
        Destino destino = primerDestino(copia);
        agregarAlumnoDePrueba(copia, destino);

        for (int i = 0; i < 2; i++) {
            assertThat(writer.actualizarCelda(copia, DNI_PRUEBA, destino.materia(), EstadoMateria.REGULAR, null, null).escrito()).isTrue();
        }
        List<HistorialExcelRow> filas = reader.leer(copia);
        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).materias()).hasSize(1);
        assertThat(filas.get(0).materias().get(0).estadoTexto()).isEqualTo("REGULAR");
    }

    // ------------------------------------------------------------------------- errores

    @Test
    void alumnoOMateriaQueNoEstanEnElArchivo_seInformanSinEscribirNada(@TempDir Path tmp) throws IOException {
        Path copia = tmp.resolve("historial-copia.xlsx");
        Files.copy(plantilla, copia);
        Destino destino = primerDestino(copia);
        agregarAlumnoDePrueba(copia, destino);
        byte[] antes = Files.readAllBytes(copia);

        HistorialExcelWriter.Resultado dniAusente = writer.actualizarCelda(copia, "11111111", destino.materia(), EstadoMateria.APROBADA, 7, null);
        HistorialExcelWriter.Resultado materiaAusente = writer.actualizarCelda(copia, DNI_PRUEBA, "MATERIA INEXISTENTE", EstadoMateria.APROBADA, 7, null);

        assertThat(dniAusente.estado()).isEqualTo(HistorialExcelWriter.Resultado.Estado.NO_ESTA_EN_EL_ARCHIVO);
        assertThat(materiaAusente.estado()).isEqualTo(HistorialExcelWriter.Resultado.Estado.NO_ESTA_EN_EL_ARCHIVO);
        assertThat(dniAusente.mensaje()).contains("11111111");
        assertThat(Files.readAllBytes(copia)).isEqualTo(antes);
    }

    @Test
    void archivoInexistente_seInforma(@TempDir Path tmp) {
        HistorialExcelWriter.Resultado resultado = writer.actualizarCelda(tmp.resolve("no-existe.xlsx"), DNI_PRUEBA, "X", EstadoMateria.APROBADA, 7, null);
        assertThat(resultado.estado()).isEqualTo(HistorialExcelWriter.Resultado.Estado.ARCHIVO_INEXISTENTE);
        assertThat(resultado.mensaje()).contains("no-existe.xlsx");
    }

    @Test
    void archivoCorrupto_lanzaEnLecturaYSeInformaEnEscritura_sinPerderLosBytesOriginales(@TempDir Path tmp) throws IOException {
        Path corrupto = tmp.resolve("roto.xlsx");
        Files.writeString(corrupto, "esto,es,un,csv\n1,2,3,4\n");
        byte[] antes = Files.readAllBytes(corrupto);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> reader.leer(corrupto)).isInstanceOf(Exception.class);
        HistorialExcelWriter.Resultado resultado = writer.actualizarCelda(corrupto, DNI_PRUEBA, "X", EstadoMateria.APROBADA, 7, null);

        assertThat(resultado.estado()).isEqualTo(HistorialExcelWriter.Resultado.Estado.ERROR);
        assertThat(resultado.mensaje()).isNotBlank();
        assertThat(Files.readAllBytes(corrupto)).isEqualTo(antes);
        assertThat(tmp.resolve("roto.xlsx.tmp")).doesNotExist();
    }

    @Test
    void layoutDelHistorial_reconoceLasColumnasDeMateria_ytoleraLasIrregularidadesDelOriginal() throws IOException {
        try (FileInputStream in = new FileInputStream(plantilla.toFile()); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheet("PROG_R6175_03");
            HistorialExcelLayout layout = HistorialExcelLayout.leer(sheet);
            Map<String, HistorialExcelLayout.ColumnasMateria> materias = layout.getColumnasPorMateria();
            HistorialExcelLayout.ColumnasMateria alg = layout.columnas("ALG");
            assertThat(materias).containsKey("ALG");
            assertThat(alg.colFecha()).isEqualTo(alg.colCursada() + 1);
            assertThat(alg.colFinal()).isEqualTo(alg.colCursada() + 2);
            // El encabezado original trae "15 - SO FECHAS" (plural): igual se reconoce la materia
            assertThat(materias).containsKey("SO");
            // 24 materias en el plan, pero "EDI" figura 3 veces: 22 nombres distintos, uno solo por nombre
            assertThat(materias).hasSize(22);
            assertThat(layout.getProblemasDeEstructura()).hasSize(2).allSatisfy(p -> assertThat(p).contains("EDI"));
            // "8 - EDI CURSADA" es la 8.ª materia: 4 columnas fijas + 7 materias * 3 columnas = columna 25
            assertThat(layout.columnas("EDI").colCursada()).as("se usa la primera columna EDI (8 - EDI)").isEqualTo(25);
        }
        assertThat(reader.problemasDeEstructura(plantilla)).anySatisfy(p -> assertThat(p).contains("PROG_R6175_03").contains("EDI"));
    }
}
