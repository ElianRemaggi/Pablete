package com.instituto.siga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituto.siga.dto.request.ComisionRequest;
import com.instituto.siga.dto.request.EstadoAcademicoRequest;
import com.instituto.siga.dto.request.HorarioRequest;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cubre Exportacion Excel (docs/04 SS12) con JWT reales. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ExportacionControllerIT {

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

    private record AlumnoDeTest(long id, String token) {
    }

    private AlumnoDeTest crearAlumnoDeTest(String admin) throws Exception {
        String dni = "3098" + String.format("%06d", CONTADOR_DNI.incrementAndGet());
        String body = mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UsuarioRequest(dni, dni + "@x.com", "N", "A", "Abcd1234", Set.of("ALUMNO")))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();
        return new AlumnoDeTest(id, tokenPara(dni, "Abcd1234"));
    }

    private long crearMateria(String admin, String codigo) throws Exception {
        String body = mockMvc.perform(post("/api/v1/materias")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MateriaRequest(codigo, "Materia " + codigo, 80))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long crearComision(String admin, long materiaId) throws Exception {
        ComisionRequest request = new ComisionRequest(materiaId, "A", "2026-1C", 30,
                List.of(new HorarioRequest(1, LocalTime.of(18, 0), LocalTime.of(20, 0), "Aula 1")));
        String body = mockMvc.perform(post("/api/v1/comisiones")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long crearMesa(String admin, long materiaId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/mesas-examen")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MesaExamenRequest(materiaId, LocalDate.now().plusDays(30), Turno.MANANA, 20))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private void assertXlsxValido(byte[] datos) {
        org.assertj.core.api.Assertions.assertThat(datos).isNotEmpty();
        org.assertj.core.api.Assertions.assertThat(datos[0]).isEqualTo((byte) 'P');
        org.assertj.core.api.Assertions.assertThat(datos[1]).isEqualTo((byte) 'K');
    }

    @Test
    void alumno_noPuedeExportar_devuelve403() throws Exception {
        String alumno = tokenPara("30333444", "Alumno1234");

        mockMvc.perform(get("/api/v1/exportaciones/alumnos").header("Authorization", "Bearer " + alumno))
                .andExpect(status().isForbidden());
    }

    @Test
    void preceptor_exportaInscripcionesCursada_devuelveXlsxValido() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        String preceptor = tokenPara("30222333", "Preceptor1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);
        long materiaId = crearMateria(admin, "EXP-01");
        long comisionId = crearComision(admin, materiaId);

        mockMvc.perform(post("/api/v1/inscripciones-cursada")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comisionId\":" + comisionId + "}"))
                .andExpect(status().isOk());

        byte[] xlsx = mockMvc.perform(get("/api/v1/exportaciones/inscripciones-cursada")
                        .param("comisionId", String.valueOf(comisionId))
                        .header("Authorization", "Bearer " + preceptor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertXlsxValido(xlsx);
    }

    @Test
    void admin_exportaInscripcionesCursada_conComisionInexistente_devuelve404() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");

        mockMvc.perform(get("/api/v1/exportaciones/inscripciones-cursada")
                        .param("comisionId", "999999")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void admin_exportaInscripcionesFinal_devuelveXlsxValido() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);
        long materiaId = crearMateria(admin, "EXP-02");
        mockMvc.perform(put("/api/v1/alumnos/" + alumno.id() + "/estado-academico/" + materiaId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EstadoAcademicoRequest(EstadoMateria.REGULAR))))
                .andExpect(status().isOk());
        long mesaId = crearMesa(admin, materiaId);

        mockMvc.perform(post("/api/v1/inscripciones-final")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mesaId\":" + mesaId + "}"))
                .andExpect(status().isOk());

        byte[] xlsx = mockMvc.perform(get("/api/v1/exportaciones/inscripciones-final")
                        .param("mesaId", String.valueOf(mesaId))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertXlsxValido(xlsx);
    }

    @Test
    void preceptor_exportaAlumnos_conFiltroDeDni_devuelveXlsxValido() throws Exception {
        String preceptor = tokenPara("30222333", "Preceptor1234");

        byte[] xlsx = mockMvc.perform(get("/api/v1/exportaciones/alumnos")
                        .param("dni", "30333444")
                        .header("Authorization", "Bearer " + preceptor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertXlsxValido(xlsx);
    }
}
