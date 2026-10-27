package com.instituto.siga.excel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituto.siga.dto.request.EstadoAcademicoRequest;
import com.instituto.siga.dto.request.LoginRequest;
import com.instituto.siga.dto.request.MateriaRequest;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.EstadoMateria;
import com.instituto.siga.entity.Rol;
import com.instituto.siga.entity.Usuario;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.EstadoAcademicoMateriaRepository;
import com.instituto.siga.repository.RolRepository;
import com.instituto.siga.repository.UsuarioRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Importacion masiva desde la interfaz (POST /historial-academico/importar) con archivos .xlsx generados en el test:
 * vista previa sin efectos, confirmacion, +500 alumnos, reimportacion idempotente, politica de conflictos, errores por
 * fila, archivos invalidos y permisos. No toca data/historial-academico.xlsx. Los datos son sinteticos de prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class HistorialImportacionIT {

    private static final AtomicInteger CONTADOR = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AlumnoRepository alumnoRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private EstadoAcademicoMateriaRepository estadoRepository;
    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    // ---------------------------------------------------------------- helpers

    private String token(String dni, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(dni, password))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private String admin() throws Exception {
        return token("30111222", "Admin1234");
    }

    private String preceptor() throws Exception {
        return token("30222333", "Preceptor1234");
    }

    private String nombreUnico(String base) {
        return base + "-" + CONTADOR.incrementAndGet();
    }

    private long crearMateria(String admin, String nombre) throws Exception {
        String body = mockMvc.perform(post("/api/v1/materias").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MateriaRequest("IM" + CONTADOR.incrementAndGet(), nombre, 100))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    /** Alumnos de prueba insertados directo (hash de relleno: no hace falta loguearse), mucho mas rapido que 520 POST. */
    private List<Alumno> crearAlumnosRapido(int cantidad, String prefijo) {
        return new org.springframework.transaction.support.TransactionTemplate(transactionManager)
                .execute(status -> crearAlumnosEnTransaccion(cantidad, prefijo));
    }

    private List<Alumno> crearAlumnosEnTransaccion(int cantidad, String prefijo) {
        Rol rol = rolRepository.findByNombre("ALUMNO").orElseThrow();
        List<Alumno> alumnos = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            String dni = prefijo + String.format("%05d", i);
            Usuario u = usuarioRepository.save(Usuario.builder().dni(dni).email(dni + "@test.local").passwordHash("x")
                    .nombre("Nombre" + i).apellido("Apellido" + i).activo(true).roles(new HashSet<>(Set.of(rol))).build());
            alumnos.add(alumnoRepository.save(Alumno.builder().usuario(u).legajo("L" + dni)
                    .fechaIngreso(java.time.LocalDate.now()).build()));
        }
        return alumnos;
    }

    /** Fila de alumno: dni, apellido, nombre, cohorte y por materia {estado, nota (Integer o String)}. */
    private record FilaXlsx(String dni, String apellido, String nombre, Object... materias) {
    }

    /** Libro con una hoja "CARRERA_TEST": fila 1 encabezado, fila 2 rotulos, alumnos desde la fila 3. */
    private byte[] xlsx(List<String> nombresMaterias, List<FilaXlsx> filas) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = wb.createSheet("CARRERA_TEST");
            Row enc = hoja.createRow(0);
            enc.createCell(0).setCellValue("DNI");
            enc.createCell(1).setCellValue("APELLIDO/S");
            enc.createCell(2).setCellValue("NOMBRE/S");
            enc.createCell(3).setCellValue("COHORTE");
            for (int m = 0; m < nombresMaterias.size(); m++) {
                int c = 4 + m * 3;
                enc.createCell(c).setCellValue((m + 1) + " - " + nombresMaterias.get(m) + " CURSADA");
                enc.createCell(c + 1).setCellValue((m + 1) + " - " + nombresMaterias.get(m) + " FECHA");
                enc.createCell(c + 2).setCellValue((m + 1) + " - " + nombresMaterias.get(m) + " FINAL");
            }
            Row rotulos = hoja.createRow(1);
            rotulos.createCell(0).setCellValue("DNI");
            int r = 2;
            for (FilaXlsx f : filas) {
                Row row = hoja.createRow(r++);
                row.createCell(0).setCellValue(f.dni());
                row.createCell(1).setCellValue(f.apellido());
                row.createCell(2).setCellValue(f.nombre());
                row.createCell(3).setCellValue(2022);
                for (int m = 0; m < f.materias().length; m += 2) {
                    int c = 4 + (m / 2) * 3;
                    Object estado = f.materias()[m];
                    Object nota = f.materias()[m + 1];
                    if (estado != null) row.createCell(c).setCellValue(estado.toString());
                    if (nota instanceof Integer n) row.createCell(c + 2).setCellValue(n);
                    else if (nota != null) row.createCell(c + 2).setCellValue(nota.toString());
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private MvcResult subir(String token, byte[] contenido, boolean confirmar, boolean crearAlumnos) throws Exception {
        return mockMvc.perform(multipart("/api/v1/historial-academico/importar")
                .file(new MockMultipartFile("archivo", "historial.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", contenido))
                .param("confirmar", String.valueOf(confirmar))
                .param("crearAlumnos", String.valueOf(crearAlumnos))
                .header("Authorization", "Bearer " + token)).andReturn();
    }

    private JsonNode ok(MvcResult r) throws Exception {
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        return objectMapper.readTree(r.getResponse().getContentAsString());
    }

    // ------------------------------------------------------------------ tests

    @Test
    void masivo_masDe500Alumnos_vistaPreviaSinEfectos_confirmacion_yReimportacionSinDuplicados() throws Exception {
        String admin = admin();
        String mat1 = nombreUnico("MASIVA-A");
        String mat2 = nombreUnico("MASIVA-B");
        long id1 = crearMateria(admin, mat1);
        long id2 = crearMateria(admin, mat2);
        List<Alumno> alumnos = crearAlumnosRapido(520, "77");
        List<FilaXlsx> filas = new ArrayList<>();
        for (Alumno a : alumnos) {
            filas.add(new FilaXlsx(a.getUsuario().getDni(), "Apellido", "Nombre", "APROBADA", 8, "REGULAR", null));
        }
        byte[] archivo = xlsx(List.of(mat1, mat2), filas);

        // Vista previa: informa todo y no modifica la base.
        JsonNode previa = ok(subir(preceptor(), archivo, false, false));
        assertThat(previa.get("confirmada").asBoolean()).isFalse();
        assertThat(previa.get("filasLeidas").asInt()).isEqualTo(520);
        assertThat(previa.get("alumnosExistentes").asInt()).isEqualTo(520);
        assertThat(previa.get("registrosCreados").asInt()).isEqualTo(1040);
        assertThat(previa.get("errores")).isEmpty();
        assertThat(previa.get("hayMasCambios").asBoolean()).isTrue(); // el listado se recorta a 500 cambios
        assertThat(estadoRepository.findAll().stream().filter(e -> e.getMateria().getId() == id1 || e.getMateria().getId() == id2))
                .isEmpty();

        // Confirmacion: crea los 1040 registros.
        JsonNode resultado = ok(subir(preceptor(), archivo, true, false));
        assertThat(resultado.get("confirmada").asBoolean()).isTrue();
        assertThat(resultado.get("registrosCreados").asInt()).isEqualTo(1040);
        assertThat(contar(id1, id2)).isEqualTo(1040);

        // Datos disponibles despues: el historial del alumno los muestra con su estado y nota.
        long alumnoId = alumnos.get(0).getUsuarioId();
        String historial = mockMvc.perform(get("/api/v1/alumnos/" + alumnoId + "/historial-academico")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(historial).contains(mat1).contains("APROBADA").contains("REGULAR");

        // Reimportar el mismo archivo: nada nuevo, nada duplicado.
        JsonNode otra = ok(subir(preceptor(), archivo, true, false));
        assertThat(otra.get("registrosCreados").asInt()).isZero();
        assertThat(otra.get("registrosActualizados").asInt()).isZero();
        assertThat(otra.get("registrosSinCambios").asInt()).isEqualTo(1040);
        assertThat(contar(id1, id2)).isEqualTo(1040);

        // Archivo parcial (10 alumnos): los 510 restantes se conservan intactos.
        JsonNode parcial = ok(subir(preceptor(), xlsx(List.of(mat1, mat2), filas.subList(0, 10)), true, false));
        assertThat(parcial.get("registrosSinCambios").asInt()).isEqualTo(20);
        assertThat(contar(id1, id2)).isEqualTo(1040);
    }

    private long contar(long... materias) {
        Set<Long> ids = new HashSet<>();
        for (long m : materias) ids.add(m);
        return estadoRepository.findAll().stream().filter(e -> ids.contains(e.getMateria().getId())).count();
    }

    @Test
    void politicaDeConflictos_noPisaNotasNiRetrocedeEstados_yActualizaLoDeMayorAvance() throws Exception {
        String admin = admin();
        String matA = nombreUnico("CONF-A");
        String matB = nombreUnico("CONF-B");
        String matC = nombreUnico("CONF-C");
        long idA = crearMateria(admin, matA);
        long idB = crearMateria(admin, matB);
        long idC = crearMateria(admin, matC);
        Alumno alumno = crearAlumnosRapido(1, "78").get(0);
        String dni = alumno.getUsuario().getDni();
        long alumnoId = alumno.getUsuarioId();
        // En la base: A aprobada con nota 8, B cursando, C regular.
        fijar(admin, alumnoId, idA, EstadoMateria.APROBADA, 8);
        fijar(admin, alumnoId, idB, EstadoMateria.CURSANDO, null);
        fijar(admin, alumnoId, idC, EstadoMateria.REGULAR, null);

        // El Excel dice: A regular (retroceso), B aprobada 9 (avance), C aprobada con nota 6 (avance).
        byte[] archivo = xlsx(List.of(matA, matB, matC),
                List.of(new FilaXlsx(dni, "Ap", "No", "REGULAR", null, "APROBADA", 9, "APROBADA", 6)));
        JsonNode previa = ok(subir(preceptor(), archivo, false, false));
        assertThat(previa.get("registrosEnConflicto").asInt()).isEqualTo(1);
        assertThat(previa.get("registrosActualizados").asInt()).isEqualTo(2);
        ok(subir(preceptor(), archivo, true, false));

        assertThat(estadoRepository.findByAlumnoIdAndMateriaId(alumnoId, idA).orElseThrow().getEstado()).isEqualTo(EstadoMateria.APROBADA);
        assertThat(estadoRepository.findByAlumnoIdAndMateriaId(alumnoId, idA).orElseThrow().getNota()).isEqualTo(8);
        assertThat(estadoRepository.findByAlumnoIdAndMateriaId(alumnoId, idB).orElseThrow().getEstado()).isEqualTo(EstadoMateria.APROBADA);
        assertThat(estadoRepository.findByAlumnoIdAndMateriaId(alumnoId, idB).orElseThrow().getNota()).isEqualTo(9);
        assertThat(estadoRepository.findByAlumnoIdAndMateriaId(alumnoId, idC).orElseThrow().getNota()).isEqualTo(6);

        // Una nota distinta sobre una aprobada con nota ya cargada es conflicto y no la pisa.
        byte[] otraNota = xlsx(List.of(matB), List.of(new FilaXlsx(dni, "Ap", "No", "APROBADA", 4)));
        JsonNode r = ok(subir(preceptor(), otraNota, true, false));
        assertThat(r.get("registrosEnConflicto").asInt()).isEqualTo(1);
        assertThat(estadoRepository.findByAlumnoIdAndMateriaId(alumnoId, idB).orElseThrow().getNota()).isEqualTo(9);
    }

    private void fijar(String admin, long alumnoId, long materiaId, EstadoMateria estado, Integer nota) throws Exception {
        mockMvc.perform(put("/api/v1/alumnos/" + alumnoId + "/estado-academico/" + materiaId)
                        .header("Authorization", "Bearer " + admin).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EstadoAcademicoRequest(estado, nota))))
                .andExpect(status().isOk());
    }

    @Test
    void erroresPorFila_seInformanSinPerderLoValido() throws Exception {
        String admin = admin();
        String mat = nombreUnico("ERR-A");
        long id = crearMateria(admin, mat);
        Alumno ok1 = crearAlumnosRapido(1, "79").get(0);
        Alumno ok2 = crearAlumnosRapido(1, "80").get(0);
        Alumno ok3 = crearAlumnosRapido(1, "81").get(0);
        Alumno ok4 = crearAlumnosRapido(1, "82").get(0);

        byte[] archivo = xlsx(List.of(mat, "MATERIA-INEXISTENTE-XYZ"), List.of(
                new FilaXlsx(ok1.getUsuario().getDni(), "A", "A", "APROBADA", 7, "APROBADA", 5),   // valida + materia inexistente
                new FilaXlsx(ok2.getUsuario().getDni(), "B", "B", "APROBADA", 11),                  // nota fuera de rango
                new FilaXlsx(ok3.getUsuario().getDni(), "C", "C", "APROBADA", "abc"),               // nota no numerica
                new FilaXlsx(ok4.getUsuario().getDni(), "D", "D", "EQUIVALENCIA", null),            // estado no modelado
                new FilaXlsx(ok1.getUsuario().getDni(), "A", "A", "REGULAR", null),                 // DNI duplicado en la hoja
                new FilaXlsx("12", "E", "E", "APROBADA", 7),                                        // DNI invalido
                new FilaXlsx("55555551", "F", "F", "APROBADA", 7)));                                // alumno inexistente
        JsonNode r = ok(subir(preceptor(), archivo, true, false));

        assertThat(r.get("registrosCreados").asInt()).isEqualTo(1);
        assertThat(r.get("registrosRechazados").asInt()).isEqualTo(4); // inexistente, 11, abc, EQUIVALENCIA
        assertThat(r.get("filasRechazadas").asInt()).isEqualTo(3);     // duplicado, dni invalido, alumno inexistente
        String errores = r.get("errores").toString();
        assertThat(errores).contains("La materia no existe en SIGA").contains("entre 1 y 10").contains("no es un número")
                .contains("no reconocido").contains("duplicado en la misma hoja").contains("DNI inválido")
                .contains("no existe en SIGA");
        // Lo valido se guardo, lo invalido no dejo nada.
        assertThat(estadoRepository.findByAlumnoIdAndMateriaId(ok1.getUsuarioId(), id).orElseThrow().getNota()).isEqualTo(7);
        assertThat(estadoRepository.findByAlumnoIdAndMateriaId(ok2.getUsuarioId(), id)).isEmpty();
        assertThat(estadoRepository.findByAlumnoIdAndMateriaId(ok3.getUsuarioId(), id)).isEmpty();
        assertThat(estadoRepository.findByAlumnoIdAndMateriaId(ok4.getUsuarioId(), id)).isEmpty();
    }

    @Test
    void lasMateriasSeEmparejanSinTildesNiMayusculas() throws Exception {
        String admin = admin();
        String base = "BIOLOGIA-T" + CONTADOR.incrementAndGet();
        long id = crearMateria(admin, base);
        Alumno alumno = crearAlumnosRapido(1, "84").get(0);
        byte[] archivo = xlsx(List.of(base.replace("IO", "IÓ").toLowerCase()),
                List.of(new FilaXlsx(alumno.getUsuario().getDni(), "A", "A", "REGULAR", null)));

        JsonNode r = ok(subir(preceptor(), archivo, true, false));

        assertThat(r.get("registrosCreados").asInt()).isEqualTo(1);
        assertThat(estadoRepository.findByAlumnoIdAndMateriaId(alumno.getUsuarioId(), id)).isPresent();
    }

    @Test
    void crearAlumnos_daDeAltaLosQueNoExisten_soloSiSePide() throws Exception {
        String admin = admin();
        String mat = nombreUnico("ALTA-A");
        long id = crearMateria(admin, mat);
        String dni = "66" + String.format("%06d", CONTADOR.incrementAndGet());
        byte[] archivo = xlsx(List.of(mat), List.of(new FilaXlsx(dni, "Nuevo", "Alumno", "REGULAR", null)));

        JsonNode sinOpcion = ok(subir(preceptor(), archivo, true, false));
        assertThat(sinOpcion.get("filasRechazadas").asInt()).isEqualTo(1);
        assertThat(usuarioRepository.findByDni(dni)).isEmpty();

        JsonNode previa = ok(subir(preceptor(), archivo, false, true));
        assertThat(previa.get("alumnosNuevos").asInt()).isEqualTo(1);
        assertThat(usuarioRepository.findByDni(dni)).isEmpty();

        JsonNode creado = ok(subir(preceptor(), archivo, true, true));
        assertThat(creado.get("alumnosNuevos").asInt()).isEqualTo(1);
        Usuario u = usuarioRepository.findByDni(dni).orElseThrow();
        assertThat(u.getApellido()).isEqualTo("Nuevo");
        assertThat(alumnoRepository.findById(u.getId())).isPresent();
        assertThat(estadoRepository.findByAlumnoIdAndMateriaId(u.getId(), id).orElseThrow().getEstado()).isEqualTo(EstadoMateria.REGULAR);
        assertThat(token(dni, dni)).isNotBlank(); // clave inicial = DNI (documentado)

        // Reimportar no duplica ni al alumno ni a sus registros.
        JsonNode otra = ok(subir(preceptor(), archivo, true, true));
        assertThat(otra.get("alumnosNuevos").asInt()).isZero();
        assertThat(otra.get("registrosSinCambios").asInt()).isEqualTo(1);
    }

    @Test
    void archivosInvalidos_seRechazanConMensajeClaro_yNoModificanNada() throws Exception {
        String admin = admin();
        long antes = estadoRepository.count();

        MvcResult vacio = subir(admin, new byte[0], true, false);
        assertThat(vacio.getResponse().getStatus()).isEqualTo(422);
        assertThat(objectMapper.readTree(vacio.getResponse().getContentAsString()).get("codigo").asText()).isEqualTo("ARCHIVO_INVALIDO");

        MvcResult basura = subir(admin, "esto no es un excel".getBytes(), true, false);
        assertThat(basura.getResponse().getStatus()).isEqualTo(422);
        assertThat(objectMapper.readTree(basura.getResponse().getContentAsString()).get("codigo").asText()).isEqualTo("ARCHIVO_INVALIDO");

        // Libro valido pero sin la columna DNI ni materias: columnas obligatorias faltantes.
        byte[] sinColumnas;
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.createSheet("HOJA").createRow(0).createCell(0).setCellValue("NOMBRE");
            wb.write(out);
            sinColumnas = out.toByteArray();
        }
        MvcResult faltan = subir(admin, sinColumnas, true, false);
        assertThat(faltan.getResponse().getStatus()).isEqualTo(422);
        JsonNode cuerpo = objectMapper.readTree(faltan.getResponse().getContentAsString());
        assertThat(cuerpo.get("codigo").asText()).isEqualTo("ARCHIVO_SIN_DATOS");
        assertThat(cuerpo.get("detalles").toString()).contains("falta la columna obligatoria DNI");

        // Extension equivocada.
        MvcResult txt = mockMvc.perform(multipart("/api/v1/historial-academico/importar")
                .file(new MockMultipartFile("archivo", "datos.csv", "text/csv", "a,b".getBytes()))
                .header("Authorization", "Bearer " + admin)).andReturn();
        assertThat(txt.getResponse().getStatus()).isEqualTo(422);

        assertThat(estadoRepository.count()).isEqualTo(antes);
    }

    @Test
    void permisos_soloPersonalPuedeImportar() throws Exception {
        String admin = admin();
        String mat = nombreUnico("PERM-A");
        crearMateria(admin, mat);
        Alumno a = crearAlumnosRapido(1, "83").get(0);
        // Un alumno real con token: se crea por API para poder loguearse.
        String dni = "3096" + String.format("%06d", CONTADOR.incrementAndGet());
        mockMvc.perform(post("/api/v1/usuarios").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.instituto.siga.dto.request.UsuarioRequest(
                                dni, dni + "@x.com", "N", "A", "Abcd1234", Set.of("ALUMNO")))))
                .andExpect(status().isOk());
        byte[] archivo = xlsx(List.of(mat), List.of(new FilaXlsx(a.getUsuario().getDni(), "A", "A", "APROBADA", 9)));

        assertThat(subir(token(dni, "Abcd1234"), archivo, true, false).getResponse().getStatus()).isEqualTo(403);
        assertThat(mockMvc.perform(multipart("/api/v1/historial-academico/importar")
                .file(new MockMultipartFile("archivo", "h.xlsx", "application/octet-stream", archivo))).andReturn()
                .getResponse().getStatus()).isEqualTo(401);
        assertThat(estadoRepository.findByAlumnoId(a.getUsuarioId())).isEmpty();
        assertThat(subir(preceptor(), archivo, true, false).getResponse().getStatus()).isEqualTo(200);
    }
}
