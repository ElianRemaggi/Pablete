package com.instituto.siga.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituto.siga.dto.request.CalificacionCursadaRequest;
import com.instituto.siga.dto.request.ComisionRequest;
import com.instituto.siga.dto.request.CorrelatividadRequest;
import com.instituto.siga.dto.request.HorarioRequest;
import com.instituto.siga.dto.request.InscripcionCursadaRequest;
import com.instituto.siga.dto.request.LoginRequest;
import com.instituto.siga.dto.request.MateriaRequest;
import com.instituto.siga.dto.request.MesaExamenRequest;
import com.instituto.siga.dto.request.ResultadoFinalRequest;
import com.instituto.siga.dto.request.UsuarioRequest;
import com.instituto.siga.entity.Modalidad;
import com.instituto.siga.entity.ResultadoCursada;
import com.instituto.siga.entity.ResultadoFinal;
import com.instituto.siga.entity.TipoRequisito;
import com.instituto.siga.entity.Turno;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo inscripcion a cursada -> nota de cursada -> habilitacion del final -> resultado del final, con JWT reales y
 * llamando directo a los endpoints (las reglas viven en el backend). Cada test arma sus propias materias y alumnos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AcreditacionCursadasFinalesIT {

    private static final AtomicInteger CONTADOR = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

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

    private record Alumno(long id, String dni, String token) {
    }

    private Alumno crearAlumno(String admin) throws Exception {
        String dni = "3095" + String.format("%06d", CONTADOR.incrementAndGet());
        String body = mockMvc.perform(post("/api/v1/usuarios").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UsuarioRequest(dni, dni + "@x.com", "Nombre", "Apellido", "Abcd1234", Set.of("ALUMNO")))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new Alumno(objectMapper.readTree(body).get("id").asLong(), dni, token(dni, "Abcd1234"));
    }

    private long crearMateria(String admin, String nombre) throws Exception {
        String codigo = "AC-" + CONTADOR.incrementAndGet();
        String body = mockMvc.perform(post("/api/v1/materias").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MateriaRequest(codigo, nombre + " " + codigo, 100))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long crearComision(String admin, long materiaId, int dia) throws Exception {
        String body = mockMvc.perform(post("/api/v1/comisiones").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ComisionRequest(materiaId, "A", "2026-1C", 30, null,
                                List.of(new HorarioRequest(dia, LocalTime.of(8, 0), LocalTime.of(10, 0), "Aula 1"))))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private void correlatividad(String admin, long materia, long requerida, TipoRequisito tipo) throws Exception {
        mockMvc.perform(post("/api/v1/correlatividades").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CorrelatividadRequest(materia, requerida, tipo))))
                .andExpect(status().isOk());
    }

    private long crearMesaPublicada(String admin, long materiaId) throws Exception {
        MesaExamenRequest req = new MesaExamenRequest(materiaId, LocalDate.now().plusDays(30 + CONTADOR.incrementAndGet() % 300),
                Turno.MANANA, 20, "Prof. Test", LocalTime.of(9, 0), "2026-2", "Aula 7", Modalidad.PRESENCIAL, null, null);
        String body = mockMvc.perform(post("/api/v1/mesas-examen").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();
        mockMvc.perform(patch("/api/v1/mesas-examen/" + id + "/publicar").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        return id;
    }

    private long inscribirCursada(Alumno alumno, long comisionId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/inscripciones-cursada").header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InscripcionCursadaRequest(comisionId))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private MvcResult calificarCursada(String token, long inscripcionId, ResultadoCursada resultado, Integer nota) throws Exception {
        return mockMvc.perform(put("/api/v1/inscripciones-cursada/" + inscripcionId + "/calificacion")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CalificacionCursadaRequest(resultado, nota)))).andReturn();
    }

    private MvcResult inscribirFinal(Alumno alumno, long mesaId) throws Exception {
        return mockMvc.perform(post("/api/v1/inscripciones-final").header("Authorization", "Bearer " + alumno.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"mesaId\":" + mesaId + "}")).andReturn();
    }

    private MvcResult resultadoFinal(String token, long inscripcionFinalId, ResultadoFinal resultado, Integer nota) throws Exception {
        return mockMvc.perform(put("/api/v1/inscripciones-final/" + inscripcionFinalId + "/resultado")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ResultadoFinalRequest(resultado, nota)))).andReturn();
    }

    private JsonNode json(MvcResult r) throws Exception {
        return objectMapper.readTree(r.getResponse().getContentAsString());
    }

    private long idDe(MvcResult r) throws Exception {
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        return json(r).get("id").asLong();
    }

    // ------------------------------------------------------------------ cursadas

    @Test
    void cursadaAprobada_persisteSeVeYHabilitaElFinal() throws Exception {
        String admin = admin();
        String preceptor = preceptor();
        Alumno alumno = crearAlumno(admin);
        long mat = crearMateria(admin, "Matematica I");
        long comision = crearComision(admin, mat, 1);
        long mesa = crearMesaPublicada(admin, mat);

        long inscripcion = inscribirCursada(alumno, comision);

        // Sin nota de cursada todavia: el final esta bloqueado y el mensaje lo explica.
        MvcResult bloqueado = inscribirFinal(alumno, mesa);
        assertThat(bloqueado.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(bloqueado).get("codigo").asText()).isEqualTo("MATERIA_NO_REGULARIZADA");
        assertThat(json(bloqueado).get("mensaje").asText()).contains("todavía no tenés aprobada la cursada");

        // El personal encuentra al alumno por DNI y ve su inscripcion en la comision.
        mockMvc.perform(get("/api/v1/alumnos").param("dni", alumno.dni()).header("Authorization", "Bearer " + preceptor))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/inscripciones-cursada/comision/" + comision).header("Authorization", "Bearer " + preceptor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].alumnoDni").value(alumno.dni()))
                .andExpect(jsonPath("$[0].resultadoCursada").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].periodo").value("2026-1C"));

        // Se carga la nota: queda guardada y asociada a alumno/materia/comision/periodo.
        MvcResult ok = calificarCursada(preceptor, inscripcion, ResultadoCursada.APROBADA, 7);
        assertThat(ok.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(ok).get("notaCursada").asInt()).isEqualTo(7);
        assertThat(json(ok).get("resultadoCursada").asText()).isEqualTo("APROBADA");

        // El alumno la ve y queda habilitado para el final.
        mockMvc.perform(get("/api/v1/inscripciones-cursada/me").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notaCursada").value(7))
                .andExpect(jsonPath("$[0].resultadoCursada").value("APROBADA"));
        mockMvc.perform(get("/api/v1/alumnos/me/materias/" + mat + "/habilitacion-final")
                        .header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cursadaAprobada").value(true))
                .andExpect(jsonPath("$.habilitado").value(true));

        // Ahora si puede inscribirse al final; la cursada aprobada NO marco el final como aprobado.
        assertThat(inscribirFinal(alumno, mesa).getResponse().getStatus()).isEqualTo(200);
        mockMvc.perform(get("/api/v1/alumnos/me/materias-aprobadas").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void cursadaDesaprobadaOPendiente_noHabilitaElFinal_yUnaCorreccionSiLoRevierte() throws Exception {
        String admin = admin();
        String preceptor = preceptor();
        Alumno alumno = crearAlumno(admin);
        long mat = crearMateria(admin, "Fisica");
        long comision = crearComision(admin, mat, 2);
        long mesa = crearMesaPublicada(admin, mat);
        long inscripcion = inscribirCursada(alumno, comision);

        assertThat(calificarCursada(preceptor, inscripcion, ResultadoCursada.DESAPROBADA, 3).getResponse().getStatus()).isEqualTo(200);
        assertThat(inscribirFinal(alumno, mesa).getResponse().getStatus()).isEqualTo(422);

        assertThat(calificarCursada(preceptor, inscripcion, ResultadoCursada.PENDIENTE, null).getResponse().getStatus()).isEqualTo(200);
        assertThat(inscribirFinal(alumno, mesa).getResponse().getStatus()).isEqualTo(422);

        // Aprobada habilita; corregida a desaprobada vuelve a bloquear las inscripciones futuras (sin borrar nada).
        assertThat(calificarCursada(preceptor, inscripcion, ResultadoCursada.APROBADA, 6).getResponse().getStatus()).isEqualTo(200);
        mockMvc.perform(get("/api/v1/alumnos/me/materias/" + mat + "/habilitacion-final")
                        .header("Authorization", "Bearer " + alumno.token()))
                .andExpect(jsonPath("$.habilitado").value(true));
        assertThat(calificarCursada(preceptor, inscripcion, ResultadoCursada.DESAPROBADA, 2).getResponse().getStatus()).isEqualTo(200);
        mockMvc.perform(get("/api/v1/alumnos/me/materias/" + mat + "/habilitacion-final")
                        .header("Authorization", "Bearer " + alumno.token()))
                .andExpect(jsonPath("$.habilitado").value(false))
                .andExpect(jsonPath("$.pendientes[0]").value("todavía no tenés aprobada la cursada"));
        mockMvc.perform(get("/api/v1/inscripciones-cursada/me").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(jsonPath("$[0].resultadoCursada").value("DESAPROBADA"));
    }

    @Test
    void calificarCursada_validaNotaYPermisos() throws Exception {
        String admin = admin();
        Alumno alumno = crearAlumno(admin);
        Alumno otro = crearAlumno(admin);
        long mat = crearMateria(admin, "Quimica");
        long inscripcion = inscribirCursada(alumno, crearComision(admin, mat, 3));

        // Un alumno no puede acreditarse la cursada, ni la de otro; sin token tampoco.
        assertThat(calificarCursada(alumno.token(), inscripcion, ResultadoCursada.APROBADA, 10).getResponse().getStatus()).isEqualTo(403);
        assertThat(calificarCursada(otro.token(), inscripcion, ResultadoCursada.APROBADA, 10).getResponse().getStatus()).isEqualTo(403);
        mockMvc.perform(put("/api/v1/inscripciones-cursada/" + inscripcion + "/calificacion")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"resultado\":\"APROBADA\",\"nota\":8}"))
                .andExpect(status().isUnauthorized());
        // Y no queda nada acreditado.
        mockMvc.perform(get("/api/v1/inscripciones-cursada/me").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(jsonPath("$[0].resultadoCursada").value("PENDIENTE"));

        // Sin nota valida no hay cursada aprobada/desaprobada; fuera de 1..10 es 400; inscripcion inexistente es 404.
        MvcResult sinNota = calificarCursada(admin, inscripcion, ResultadoCursada.APROBADA, null);
        assertThat(sinNota.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(sinNota).get("codigo").asText()).isEqualTo("NOTA_INVALIDA");
        assertThat(calificarCursada(admin, inscripcion, ResultadoCursada.PENDIENTE, 5).getResponse().getStatus()).isEqualTo(422);
        assertThat(calificarCursada(admin, inscripcion, ResultadoCursada.APROBADA, 11).getResponse().getStatus()).isEqualTo(400);
        assertThat(calificarCursada(admin, 99999999L, ResultadoCursada.APROBADA, 8).getResponse().getStatus()).isEqualTo(404);
    }

    // ------------------------------------------------------------- correlatividades

    @Test
    void matematicaII_exigeCursadasYFinalDeMatematicaIAprobado() throws Exception {
        String admin = admin();
        String preceptor = preceptor();
        Alumno alumno = crearAlumno(admin);
        long mat1 = crearMateria(admin, "Matematica I");
        long mat2 = crearMateria(admin, "Matematica II");
        correlatividad(admin, mat2, mat1, TipoRequisito.CURSAR);
        long comision1 = crearComision(admin, mat1, 1);
        long comision2 = crearComision(admin, mat2, 2);
        long mesa1 = crearMesaPublicada(admin, mat1);
        long mesa2 = crearMesaPublicada(admin, mat2);

        // No se puede cursar Mat II sin la cursada de Mat I.
        MvcResult sinCursadaI = mockMvc.perform(post("/api/v1/inscripciones-cursada")
                .header("Authorization", "Bearer " + alumno.token()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new InscripcionCursadaRequest(comision2)))).andReturn();
        assertThat(sinCursadaI.getResponse().getStatus()).isEqualTo(422);

        long insc1 = inscribirCursada(alumno, comision1);
        calificarCursada(preceptor, insc1, ResultadoCursada.APROBADA, 8);
        long insc2 = inscribirCursada(alumno, comision2);
        calificarCursada(preceptor, insc2, ResultadoCursada.APROBADA, 7);

        // Cursadas I y II aprobadas, final I sin aprobar: el final de II esta bloqueado y dice por que.
        MvcResult bloqueado = inscribirFinal(alumno, mesa2);
        assertThat(bloqueado.getResponse().getStatus()).isEqualTo(422);
        assertThat(json(bloqueado).get("codigo").asText()).isEqualTo("REGLA_NEGOCIO_CORRELATIVIDAD");
        assertThat(json(bloqueado).get("mensaje").asText())
                .contains("No podés inscribirte al final de").contains("todavía no tenés aprobado el final de Matematica I");
        mockMvc.perform(get("/api/v1/alumnos/me/materias/" + mat2 + "/habilitacion-final")
                        .header("Authorization", "Bearer " + alumno.token()))
                .andExpect(jsonPath("$.cursadaAprobada").value(true))
                .andExpect(jsonPath("$.habilitado").value(false))
                .andExpect(jsonPath("$.correlatividades[0].cumplido").value(false));

        // Final I desaprobado: el requisito sigue pendiente.
        long finalI = idDe(inscribirFinal(alumno, mesa1));
        assertThat(resultadoFinal(preceptor, finalI, ResultadoFinal.DESAPROBADO, 3).getResponse().getStatus()).isEqualTo(200);
        assertThat(inscribirFinal(alumno, mesa2).getResponse().getStatus()).isEqualTo(422);

        // Final I aprobado en un segundo intento: habilita el final de II.
        long mesa1b = crearMesaPublicada(admin, mat1);
        long finalI2 = idDe(inscribirFinal(alumno, mesa1b));
        MvcResult aprobado = resultadoFinal(preceptor, finalI2, ResultadoFinal.APROBADO, 9);
        assertThat(aprobado.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(aprobado).get("resultado").asText()).isEqualTo("APROBADO");
        assertThat(json(aprobado).get("nota").asInt()).isEqualTo(9);
        assertThat(inscribirFinal(alumno, mesa2).getResponse().getStatus()).isEqualTo(200);

        // El alumno ve sus resultados (el desaprobado y el aprobado) y la materia aprobada.
        JsonNode misFinales = objectMapper.readTree(mockMvc.perform(get("/api/v1/inscripciones-final/me")
                        .header("Authorization", "Bearer " + alumno.token()))
                .andReturn().getResponse().getContentAsString());
        for (JsonNode f : misFinales) {
            if (f.get("id").asLong() == finalI) {
                assertThat(f.get("resultado").asText()).isEqualTo("DESAPROBADO");
            }
            if (f.get("id").asLong() == finalI2) {
                assertThat(f.get("nota").asInt()).isEqualTo(9);
            }
        }
        mockMvc.perform(get("/api/v1/alumnos/me/materias-aprobadas").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(jsonPath("$.length()").value(1));

        // Corregir el final I a desaprobado revoca la correlativa para inscripciones futuras (historial intacto).
        assertThat(resultadoFinal(preceptor, finalI2, ResultadoFinal.DESAPROBADO, 2).getResponse().getStatus()).isEqualTo(200);
        mockMvc.perform(get("/api/v1/alumnos/me/materias/" + mat2 + "/habilitacion-final")
                        .header("Authorization", "Bearer " + alumno.token()))
                .andExpect(jsonPath("$.correlatividades[0].cumplido").value(false));
    }

    @Test
    void otroRequisitoPendiente_siguePendienteAunqueSeCumplaElPrimero() throws Exception {
        String admin = admin();
        String preceptor = preceptor();
        Alumno alumno = crearAlumno(admin);
        long a = crearMateria(admin, "Materia A");
        long b = crearMateria(admin, "Materia B");
        long c = crearMateria(admin, "Materia C");
        correlatividad(admin, c, a, TipoRequisito.CURSAR);
        correlatividad(admin, c, b, TipoRequisito.CURSAR);
        long insc = inscribirCursada(alumno, crearComision(admin, a, 1));
        calificarCursada(preceptor, insc, ResultadoCursada.APROBADA, 8);
        long mesaA = crearMesaPublicada(admin, a);
        long finalA = idDe(inscribirFinal(alumno, mesaA));
        resultadoFinal(preceptor, finalA, ResultadoFinal.APROBADO, 8);

        // Falta B: no puede ni cursar C.
        MvcResult r = mockMvc.perform(post("/api/v1/inscripciones-cursada")
                .header("Authorization", "Bearer " + alumno.token()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new InscripcionCursadaRequest(crearComision(admin, c, 2))))).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(422);
        mockMvc.perform(get("/api/v1/alumnos/me/materias/" + c + "/habilitacion-final")
                        .header("Authorization", "Bearer " + alumno.token()))
                .andExpect(jsonPath("$.habilitado").value(false))
                .andExpect(jsonPath("$.correlatividades.length()").value(2));
    }

    // ----------------------------------------------------------------- finales

    @Test
    void resultadoDeFinal_validaPermisosYNota() throws Exception {
        String admin = admin();
        Alumno alumno = crearAlumno(admin);
        long mat = crearMateria(admin, "Historia");
        long insc = inscribirCursada(alumno, crearComision(admin, mat, 4));
        calificarCursada(admin, insc, ResultadoCursada.APROBADA, 5);
        long mesa = crearMesaPublicada(admin, mat);
        long inscFinal = idDe(inscribirFinal(alumno, mesa));

        assertThat(resultadoFinal(alumno.token(), inscFinal, ResultadoFinal.APROBADO, 10).getResponse().getStatus()).isEqualTo(403);
        MvcResult sinNota = resultadoFinal(admin, inscFinal, ResultadoFinal.APROBADO, null);
        assertThat(sinNota.getResponse().getStatus()).isEqualTo(422);
        assertThat(resultadoFinal(admin, inscFinal, ResultadoFinal.AUSENTE, 4).getResponse().getStatus()).isEqualTo(422);
        assertThat(resultadoFinal(admin, inscFinal, ResultadoFinal.AUSENTE, null).getResponse().getStatus()).isEqualTo(200);
        mockMvc.perform(get("/api/v1/inscripciones-final/me").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(jsonPath("$[0].resultado").value("AUSENTE"));
        // Un ausente no aprueba la materia.
        mockMvc.perform(get("/api/v1/alumnos/me/materias-aprobadas").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(jsonPath("$").isEmpty());
    }
}
