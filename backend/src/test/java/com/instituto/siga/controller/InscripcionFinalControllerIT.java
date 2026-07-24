package com.instituto.siga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituto.siga.dto.request.EstadoAcademicoRequest;
import com.instituto.siga.dto.request.LoginRequest;
import com.instituto.siga.dto.request.MateriaRequest;
import com.instituto.siga.dto.request.MesaExamenRequest;
import com.instituto.siga.dto.request.UsuarioRequest;
import com.instituto.siga.entity.EstadoMateria;
import com.instituto.siga.entity.Turno;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pipeline completo de Inscripcion a Final (docs/04 SS10, docs/07 SS2) con JWT reales.
 *
 * Igual que InscripcionCursadaControllerIT: cada test crea su propio alumno para no compartir
 * estado (mesas/intentos) con otros metodos de test en este contexto sin rollback entre ellos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class InscripcionFinalControllerIT {

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

    private long crearMesa(String admin, long materiaId, LocalDate fecha, Turno turno, int cupo) throws Exception {
        String body = mockMvc.perform(post("/api/v1/mesas-examen")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MesaExamenRequest(materiaId, fecha, turno, cupo))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private record AlumnoDeTest(long id, String token) {
    }

    private AlumnoDeTest crearAlumnoDeTest(String admin) throws Exception {
        String dni = "3092" + String.format("%06d", CONTADOR_DNI.incrementAndGet());
        String body = mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UsuarioRequest(dni, dni + "@x.com", "N", "A", "Abcd1234", Set.of("ALUMNO")))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();
        return new AlumnoDeTest(id, tokenPara(dni, "Abcd1234"));
    }

    private void fijarRegular(String admin, long alumnoId, long materiaId) throws Exception {
        mockMvc.perform(put("/api/v1/alumnos/" + alumnoId + "/estado-academico/" + materiaId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EstadoAcademicoRequest(EstadoMateria.REGULAR))))
                .andExpect(status().isOk());
    }

    @Test
    void flujoCompleto_inscribirModificarYDarDeBaja() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);

        long materiaId = crearMateria(admin, "IFI-01");
        fijarRegular(admin, alumno.id(), materiaId);

        long mesaA = crearMesa(admin, materiaId, LocalDate.now().plusDays(30), Turno.MANANA, 10);
        long mesaB = crearMesa(admin, materiaId, LocalDate.now().plusDays(40), Turno.TARDE, 10);

        String body = mockMvc.perform(post("/api/v1/inscripciones-final")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mesaId\":" + mesaA + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INSCRIPTO"))
                .andReturn().getResponse().getContentAsString();
        long inscripcionId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/api/v1/inscripciones-final/me").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].materiaNombre").exists());

        mockMvc.perform(put("/api/v1/inscripciones-final/" + inscripcionId)
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mesaId\":" + mesaB + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("MODIFICADA"));

        mockMvc.perform(delete("/api/v1/inscripciones-final/" + inscripcionId)
                        .header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/inscripciones-final/me").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void inscribir_sinRegularizar_devuelve422() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);

        long materiaId = crearMateria(admin, "IFI-02");
        long mesa = crearMesa(admin, materiaId, LocalDate.now().plusDays(30), Turno.MANANA, 10);

        mockMvc.perform(post("/api/v1/inscripciones-final")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mesaId\":" + mesa + "}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("MATERIA_NO_REGULARIZADA"));
    }

    @Test
    void inscribir_conAntelacionInsuficiente_devuelve422() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);

        long materiaId = crearMateria(admin, "IFI-03");
        fijarRegular(admin, alumno.id(), materiaId);
        long mesa = crearMesa(admin, materiaId, LocalDate.now(), Turno.MANANA, 10);

        mockMvc.perform(post("/api/v1/inscripciones-final")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mesaId\":" + mesa + "}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("FUERA_DE_PLAZO"));
    }

    @Test
    void inscribir_conSuperposicionDeMesas_devuelve422() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);

        long materiaA = crearMateria(admin, "IFI-04A");
        long materiaB = crearMateria(admin, "IFI-04B");
        fijarRegular(admin, alumno.id(), materiaA);
        fijarRegular(admin, alumno.id(), materiaB);

        LocalDate fecha = LocalDate.now().plusDays(30);
        long mesaA = crearMesa(admin, materiaA, fecha, Turno.MANANA, 10);
        long mesaB = crearMesa(admin, materiaB, fecha, Turno.MANANA, 10);

        mockMvc.perform(post("/api/v1/inscripciones-final")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mesaId\":" + mesaA + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/inscripciones-final")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mesaId\":" + mesaB + "}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("SUPERPOSICION_MESA"));
    }

    @Test
    void inscribir_superandoElLimiteDeIntentos_devuelve422() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);

        long materiaId = crearMateria(admin, "IFI-05");
        fijarRegular(admin, alumno.id(), materiaId);

        // No asume el limite sembrado: ConfiguracionInstitucional es un singleton global y
        // otro test (ConfiguracionInstitucionalControllerIT) puede haberlo cambiado antes de
        // este (ver el finally que restaura ahi el valor original — esto es defensa adicional).
        String configBody = mockMvc.perform(get("/api/v1/configuracion").header("Authorization", "Bearer " + admin))
                .andReturn().getResponse().getContentAsString();
        int limite = objectMapper.readTree(configBody).get("limiteIntentosFinal").asInt();

        Turno[] turnos = Turno.values();
        for (int i = 0; i < limite; i++) {
            long mesa = crearMesa(admin, materiaId, LocalDate.now().plusDays(30 + i), turnos[i % turnos.length], 10);
            mockMvc.perform(post("/api/v1/inscripciones-final")
                            .header("Authorization", "Bearer " + alumno.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mesaId\":" + mesa + "}"))
                    .andExpect(status().isOk());
        }

        long mesaDeMas = crearMesa(admin, materiaId, LocalDate.now().plusDays(30 + limite), turnos[limite % turnos.length], 10);
        mockMvc.perform(post("/api/v1/inscripciones-final")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mesaId\":" + mesaDeMas + "}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("LIMITE_INTENTOS_SUPERADO"));
    }

    @Test
    void inscribir_duplicada_devuelve409() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);

        long materiaId = crearMateria(admin, "IFI-06");
        fijarRegular(admin, alumno.id(), materiaId);
        long mesa = crearMesa(admin, materiaId, LocalDate.now().plusDays(30), Turno.MANANA, 10);

        mockMvc.perform(post("/api/v1/inscripciones-final")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mesaId\":" + mesa + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/inscripciones-final")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mesaId\":" + mesa + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("INSCRIPCION_DUPLICADA"));
    }

    @Test
    void preceptor_puedeListarPorMesa_alumnoNoPuede() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        String preceptor = tokenPara("30222333", "Preceptor1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);

        long materiaId = crearMateria(admin, "IFI-07");
        long mesa = crearMesa(admin, materiaId, LocalDate.now().plusDays(30), Turno.MANANA, 10);

        mockMvc.perform(get("/api/v1/inscripciones-final/mesa/" + mesa)
                        .header("Authorization", "Bearer " + preceptor))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/inscripciones-final/mesa/" + mesa)
                        .header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void preceptor_puedeListarPorAlumno_alumnoNoPuede() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        String preceptor = tokenPara("30222333", "Preceptor1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);

        long materiaId = crearMateria(admin, "IFI-08");
        fijarRegular(admin, alumno.id(), materiaId);
        long mesa = crearMesa(admin, materiaId, LocalDate.now().plusDays(30), Turno.MANANA, 10);

        mockMvc.perform(post("/api/v1/inscripciones-final")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mesaId\":" + mesa + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/inscripciones-final/alumno/" + alumno.id())
                        .header("Authorization", "Bearer " + preceptor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].materiaNombre").exists());

        mockMvc.perform(get("/api/v1/inscripciones-final/alumno/" + alumno.id())
                        .header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isForbidden());
    }
}
