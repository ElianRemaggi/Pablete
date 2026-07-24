package com.instituto.siga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituto.siga.dto.request.ComisionRequest;
import com.instituto.siga.dto.request.CorrelatividadRequest;
import com.instituto.siga.dto.request.EstadoAcademicoRequest;
import com.instituto.siga.dto.request.HorarioRequest;
import com.instituto.siga.dto.request.InscripcionCursadaRequest;
import com.instituto.siga.dto.request.LoginRequest;
import com.instituto.siga.dto.request.MateriaRequest;
import com.instituto.siga.dto.request.UsuarioRequest;
import com.instituto.siga.entity.EstadoMateria;
import com.instituto.siga.entity.TipoRequisito;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pipeline completo de Inscripcion a Cursada (docs/04 SS8, docs/07 SS1) con JWT reales.
 *
 * Cada test crea su PROPIO alumno (en vez de reusar el alumno1 sembrado): no hay rollback
 * entre metodos de test en este contexto compartido, asi que una inscripcion activa creada
 * por un test persiste y puede chocar (superposicion horaria, cupo) con la de otro test que
 * corra despues en cualquier orden. Un alumno nuevo por test aisla completamente ese estado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class InscripcionCursadaControllerIT {

    private static final AtomicInteger CONTADOR_DNI = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String tokenPara(String dni, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(dni, password))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private long crearMateria(String admin, String codigo) throws Exception {
        String body = mockMvc.perform(post("/api/v1/materias")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MateriaRequest(codigo, "Materia " + codigo, 100))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long crearComision(String admin, long materiaId, String nombre, List<HorarioRequest> horarios, int cupo) throws Exception {
        String body = mockMvc.perform(post("/api/v1/comisiones")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ComisionRequest(materiaId, nombre, "2026-1C", cupo, horarios))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    /** DNI/token de un alumno nuevo, exclusivo de este test — evita cualquier estado heredado. */
    private record AlumnoDeTest(long id, String token) {
    }

    private AlumnoDeTest crearAlumnoDeTest(String admin) throws Exception {
        String dni = "3091" + String.format("%06d", CONTADOR_DNI.incrementAndGet());
        String body = mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UsuarioRequest(dni, dni + "@x.com", "N", "A", "Abcd1234", Set.of("ALUMNO")))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();
        return new AlumnoDeTest(id, tokenPara(dni, "Abcd1234"));
    }

    @Test
    void flujoCompleto_inscribirModificarYDarDeBaja() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);

        long materiaId = crearMateria(admin, "ICU-01");
        long comisionA = crearComision(admin, materiaId, "A",
                List.of(new HorarioRequest(1, LocalTime.of(18, 0), LocalTime.of(20, 0), "Aula 1")), 10);
        long comisionB = crearComision(admin, materiaId, "B",
                List.of(new HorarioRequest(3, LocalTime.of(18, 0), LocalTime.of(20, 0), "Aula 2")), 10);

        String inscripcionBody = mockMvc.perform(post("/api/v1/inscripciones-cursada")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InscripcionCursadaRequest(comisionA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INSCRIPTO"))
                .andReturn().getResponse().getContentAsString();
        long inscripcionId = objectMapper.readTree(inscripcionBody).get("id").asLong();

        mockMvc.perform(get("/api/v1/inscripciones-cursada/me").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].comisionNombre").exists());

        mockMvc.perform(put("/api/v1/inscripciones-cursada/" + inscripcionId)
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InscripcionCursadaRequest(comisionB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("MODIFICADA"));

        mockMvc.perform(delete("/api/v1/inscripciones-cursada/" + inscripcionId)
                        .header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/inscripciones-cursada/me").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void inscribir_conCorrelatividadNoCumplida_devuelve422_yLuegoDeFijarEstadoFunciona() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);

        long requerida = crearMateria(admin, "ICU-02A");
        long destino = crearMateria(admin, "ICU-02B");
        mockMvc.perform(post("/api/v1/correlatividades")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CorrelatividadRequest(destino, requerida, TipoRequisito.APROBAR))))
                .andExpect(status().isOk());

        long comision = crearComision(admin, destino, "A",
                List.of(new HorarioRequest(2, LocalTime.of(18, 0), LocalTime.of(20, 0), "Aula 1")), 10);

        mockMvc.perform(post("/api/v1/inscripciones-cursada")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InscripcionCursadaRequest(comision))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO_CORRELATIVIDAD"));

        mockMvc.perform(put("/api/v1/alumnos/" + alumno.id() + "/estado-academico/" + requerida)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EstadoAcademicoRequest(EstadoMateria.APROBADA))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/inscripciones-cursada")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InscripcionCursadaRequest(comision))))
                .andExpect(status().isOk());
    }

    @Test
    void inscribir_conSuperposicionHoraria_devuelve422() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);

        long materiaId = crearMateria(admin, "ICU-03");
        long comisionA = crearComision(admin, materiaId, "A",
                List.of(new HorarioRequest(1, LocalTime.of(18, 0), LocalTime.of(22, 0), "Aula 1")), 10);
        long materiaId2 = crearMateria(admin, "ICU-03B");
        long comisionB = crearComision(admin, materiaId2, "A",
                List.of(new HorarioRequest(1, LocalTime.of(19, 0), LocalTime.of(21, 0), "Aula 2")), 10);

        mockMvc.perform(post("/api/v1/inscripciones-cursada")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InscripcionCursadaRequest(comisionA))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/inscripciones-cursada")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InscripcionCursadaRequest(comisionB))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("SUPERPOSICION_HORARIA"));
    }

    @Test
    void inscribir_conCupoAgotado_devuelve422() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        AlumnoDeTest alumnoUno = crearAlumnoDeTest(admin);
        AlumnoDeTest alumnoDos = crearAlumnoDeTest(admin);

        long materiaId = crearMateria(admin, "ICU-04");
        long comision = crearComision(admin, materiaId, "A",
                List.of(new HorarioRequest(4, LocalTime.of(18, 0), LocalTime.of(20, 0), "Aula 1")), 1);

        mockMvc.perform(post("/api/v1/inscripciones-cursada")
                        .header("Authorization", "Bearer " + alumnoUno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InscripcionCursadaRequest(comision))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/inscripciones-cursada")
                        .header("Authorization", "Bearer " + alumnoDos.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InscripcionCursadaRequest(comision))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CUPO_AGOTADO"));
    }

    @Test
    void inscribir_duplicada_devuelve409() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);

        long materiaId = crearMateria(admin, "ICU-05");
        long comision = crearComision(admin, materiaId, "A",
                List.of(new HorarioRequest(5, LocalTime.of(18, 0), LocalTime.of(20, 0), "Aula 1")), 10);

        mockMvc.perform(post("/api/v1/inscripciones-cursada")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InscripcionCursadaRequest(comision))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/inscripciones-cursada")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InscripcionCursadaRequest(comision))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("INSCRIPCION_DUPLICADA"));
    }

    @Test
    void preceptor_puedeListarPorComision_alumnoNoPuede() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        String preceptor = tokenPara("30222333", "Preceptor1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);

        long materiaId = crearMateria(admin, "ICU-06");
        long comision = crearComision(admin, materiaId, "A",
                List.of(new HorarioRequest(6, LocalTime.of(18, 0), LocalTime.of(20, 0), "Aula 1")), 10);

        mockMvc.perform(get("/api/v1/inscripciones-cursada/comision/" + comision)
                        .header("Authorization", "Bearer " + preceptor))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/inscripciones-cursada/comision/" + comision)
                        .header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void preceptor_puedeListarPorAlumno_alumnoNoPuede() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        String preceptor = tokenPara("30222333", "Preceptor1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);

        long materiaId = crearMateria(admin, "ICU-07");
        long comision = crearComision(admin, materiaId, "A",
                List.of(new HorarioRequest(3, LocalTime.of(18, 0), LocalTime.of(20, 0), "Aula 1")), 10);

        mockMvc.perform(post("/api/v1/inscripciones-cursada")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InscripcionCursadaRequest(comision))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/inscripciones-cursada/alumno/" + alumno.id())
                        .header("Authorization", "Bearer " + preceptor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].comisionNombre").exists());

        mockMvc.perform(get("/api/v1/inscripciones-cursada/alumno/" + alumno.id())
                        .header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isForbidden());
    }
}
