package com.instituto.siga.excel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sincronizacion Excel <-> base de datos de punta a punta (API real, JWT real, H2 real) sobre una COPIA
 * temporal de la plantilla: nunca toca data/historial-academico.xlsx. Usa una base H2 propia para no
 * pisar la de los demas ITs (este test necesita un contexto con otra ruta de Excel).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class HistorialExcelSyncIT {

    private static final Path ARCHIVO_TEMPORAL;

    static {
        try {
            Path dir = Files.createTempDirectory("siga-historial-it");
            dir.toFile().deleteOnExit();
            ARCHIVO_TEMPORAL = dir.resolve("historial-academico.xlsx");
            Files.copy(new HistorialExcelPath("./data/historial-academico.xlsx").resolver(), ARCHIVO_TEMPORAL,
                    StandardCopyOption.REPLACE_EXISTING);
            ARCHIVO_TEMPORAL.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("app.historial.excel-path", () -> ARCHIVO_TEMPORAL.toAbsolutePath().toString());
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:sigadb_historial;MODE=MySQL;DB_CLOSE_DELAY=-1");
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String DNI_ALUMNO = "99000001";
    private static final String DNI_DESCONOCIDO = "99000002";

    private String tokenAdmin() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"30111222\",\"password\":\"Admin1234\"}"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    private JsonNode recargar(String admin) throws Exception {
        return json(mockMvc.perform(post("/api/v1/historial-academico/recargar").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode historial(String admin, long alumnoId) throws Exception {
        return json(mockMvc.perform(get("/api/v1/alumnos/" + alumnoId + "/historial-academico").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    /** Escribe/actualiza la fila del alumno (DNI) en la primera hoja del temporal. */
    private void ponerFila(String dni, String materia, String estado, Integer nota) throws IOException {
        try (FileInputStream in = new FileInputStream(ARCHIVO_TEMPORAL.toFile()); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            HistorialExcelLayout layout = HistorialExcelLayout.leer(sheet);
            Row fila = null;
            for (int i = HistorialExcelLayout.PRIMERA_FILA_DATOS; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r != null && dni.equals(HistorialExcelCellUtils.texto(r.getCell(layout.getColumnaDni())))) {
                    fila = r;
                }
            }
            if (fila == null) {
                fila = sheet.createRow(Math.max(sheet.getLastRowNum() + 1, HistorialExcelLayout.PRIMERA_FILA_DATOS));
                fila.createCell(layout.getColumnaDni()).setCellValue(dni);
                fila.createCell(layout.getColumnaApellido()).setCellValue("TEST-EXCEL");
                fila.createCell(layout.getColumnaNombre()).setCellValue("Sync");
            }
            if (materia != null) {
                HistorialExcelLayout.ColumnasMateria c = layout.columnas(materia);
                Cell estadoCell = fila.getCell(c.colCursada()) != null ? fila.getCell(c.colCursada()) : fila.createCell(c.colCursada());
                estadoCell.setCellValue(estado);
                Cell notaCell = fila.getCell(c.colFinal()) != null ? fila.getCell(c.colFinal()) : fila.createCell(c.colFinal());
                if (nota != null) notaCell.setCellValue(nota);
            }
            try (FileOutputStream out = new FileOutputStream(ARCHIVO_TEMPORAL.toFile())) {
                wb.write(out);
            }
        }
    }

    private String primeraMateria() throws IOException {
        try (FileInputStream in = new FileInputStream(ARCHIVO_TEMPORAL.toFile()); Workbook wb = WorkbookFactory.create(in)) {
            return HistorialExcelLayout.leer(wb.getSheetAt(0)).materias().iterator().next().getValue().nombreOriginal();
        }
    }

    private String celdaEstado(String dni, String materia) throws IOException {
        try (FileInputStream in = new FileInputStream(ARCHIVO_TEMPORAL.toFile()); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            HistorialExcelLayout layout = HistorialExcelLayout.leer(sheet);
            for (int i = HistorialExcelLayout.PRIMERA_FILA_DATOS; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r != null && dni.equals(HistorialExcelCellUtils.texto(r.getCell(layout.getColumnaDni())))) {
                    return HistorialExcelCellUtils.texto(r.getCell(layout.columnas(materia).colCursada()))
                            + "/" + HistorialExcelCellUtils.texto(r.getCell(layout.columnas(materia).colFinal()));
                }
            }
        }
        return null;
    }

    @Test
    void sincronizacion_importaSinDuplicar_resuelveConflictos_yEscribeDeVuelta() throws Exception {
        String admin = tokenAdmin();
        String nombreMateria = primeraMateria();

        // Datos de prueba identificables: un alumno (DNI 99000001) y la materia que existe en el Excel
        long alumnoId = json(mockMvc.perform(post("/api/v1/usuarios").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"" + DNI_ALUMNO + "\",\"email\":\"sync99@test.com\",\"nombre\":\"Sync\",\"apellido\":\"TEST-EXCEL\","
                                + "\"password\":\"Abcd1234\",\"roles\":[\"ALUMNO\"]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        long materiaId = json(mockMvc.perform(post("/api/v1/materias").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"XL-SYNC1\",\"nombre\":\"" + nombreMateria + "\",\"cargaHoraria\":64}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();

        // 1. Un registro controlado en el Excel se importa
        ponerFila(DNI_ALUMNO, nombreMateria, "APROBADA", 8);
        JsonNode r1 = recargar(admin);
        assertThat(r1.get("archivo").asText()).isEqualTo(ARCHIVO_TEMPORAL.toAbsolutePath().toString());
        assertThat(r1.get("filasLeidas").asInt()).isEqualTo(1);
        assertThat(r1.get("actualizados").asInt()).isEqualTo(1);
        assertThat(r1.get("conflictos").asInt()).isZero();
        JsonNode h1 = historial(admin, alumnoId);
        assertThat(h1).hasSize(1);
        assertThat(h1.get(0).get("estado").asText()).isEqualTo("APROBADA");
        assertThat(h1.get(0).get("nota").asInt()).isEqualTo(8);

        // 2. Sincronizar otra vez no duplica ni "actualiza" nada
        JsonNode r2 = recargar(admin);
        assertThat(r2.get("actualizados").asInt()).isZero();
        assertThat(historial(admin, alumnoId)).hasSize(1);

        // 3. Conflicto: el Excel contradice una aprobacion de la base -> se conserva la base y se informa
        ponerFila(DNI_ALUMNO, nombreMateria, "APROBADA", 9);
        JsonNode r3 = recargar(admin);
        assertThat(r3.get("conflictos").asInt()).isEqualTo(1);
        assertThat(r3.get("advertencias").toString()).contains("CONFLICTO").contains(DNI_ALUMNO);
        assertThat(historial(admin, alumnoId).get(0).get("nota").asInt()).isEqualTo(8);

        // 4. Un DNI que no existe en el sistema se informa y no se importa
        ponerFila(DNI_DESCONOCIDO, null, null, null);
        JsonNode r4 = recargar(admin);
        assertThat(r4.get("alumnosNoEncontrados").asInt()).isEqualTo(1);
        assertThat(r4.get("advertencias").toString()).contains(DNI_DESCONOCIDO);

        // 5. Un cambio academico hecho desde SIGA se escribe en el Excel y queda persistido en el archivo
        String respuesta = mockMvc.perform(put("/api/v1/alumnos/" + alumnoId + "/estado-academico/" + materiaId)
                        .header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"APROBADA\",\"nota\":9}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json(respuesta).get("excelSincronizado").asBoolean()).as(respuesta).isTrue();
        assertThat(celdaEstado(DNI_ALUMNO, nombreMateria)).isEqualTo("APROBADA/9");
        // y ya no hay conflicto: base y Excel coinciden
        assertThat(recargar(admin).get("conflictos").asInt()).isZero();

        // 6. Un cambio sobre una materia que no figura en el Excel se guarda en la base y avisa que no se escribio
        long materiaAjena = json(mockMvc.perform(post("/api/v1/materias").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"XL-SYNC2\",\"nombre\":\"Materia que no esta en el Excel\",\"cargaHoraria\":64}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        JsonNode sinExcel = json(mockMvc.perform(put("/api/v1/alumnos/" + alumnoId + "/estado-academico/" + materiaAjena)
                        .header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"REGULAR\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(sinExcel.get("estado").asText()).isEqualTo("REGULAR");
        assertThat(sinExcel.get("excelSincronizado").asBoolean()).isFalse();
        assertThat(sinExcel.get("excelMensaje").asText()).contains("Excel");

        // 7. El archivo sigue siendo legible tras todas las escrituras
        try (FileInputStream in = new FileInputStream(ARCHIVO_TEMPORAL.toFile()); Workbook wb = WorkbookFactory.create(in)) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(9);
        }
    }

    @Test
    void recargar_requiereAdministrador() throws Exception {
        String bodyAlumno = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"30333444\",\"password\":\"Alumno1234\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(bodyAlumno).get("accessToken").asText();
        mockMvc.perform(post("/api/v1/historial-academico/recargar").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/historial-academico/recargar")).andExpect(status().isUnauthorized());
    }
}
