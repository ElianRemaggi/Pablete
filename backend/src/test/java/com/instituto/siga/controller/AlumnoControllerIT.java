package com.instituto.siga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituto.siga.dto.request.EstadoAcademicoRequest;
import com.instituto.siga.dto.request.LoginRequest;
import com.instituto.siga.dto.request.MateriaRequest;
import com.instituto.siga.dto.request.UsuarioRequest;
import com.instituto.siga.entity.EstadoMateria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cubre Alumnos + Historial Academico (docs/04 SS3) con JWT reales. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AlumnoControllerIT {

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

    private record AlumnoDeTest(long id, String dni, String token) {
    }

    private AlumnoDeTest crearAlumnoDeTest(String admin) throws Exception {
        return crearAlumnoDeTest(admin, "ApellidoIT");
    }

    private AlumnoDeTest crearAlumnoDeTest(String admin, String apellido) throws Exception {
        String dni = "3097" + String.format("%06d", CONTADOR_DNI.incrementAndGet());
        String body = mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UsuarioRequest(dni, dni + "@x.com", "Nombre", apellido, "Abcd1234", Set.of("ALUMNO")))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();
        return new AlumnoDeTest(id, dni, tokenPara(dni, "Abcd1234"));
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

    @Test
    void alumno_puedeVerSuPropiaInfo_yUnAdminNoAlumnoRecibe403EnMe() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);

        mockMvc.perform(get("/api/v1/alumnos/me").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value(alumno.dni()))
                .andExpect(jsonPath("$.legajo").exists());

        mockMvc.perform(get("/api/v1/alumnos/me").header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());
    }

    @Test
    void alumno_noPuedeVerInfoDeOtroPorId_peroPreceptorSi() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        String preceptor = tokenPara("30222333", "Preceptor1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);

        mockMvc.perform(get("/api/v1/alumnos/" + alumno.id()).header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/alumnos/" + alumno.id()).header("Authorization", "Bearer " + preceptor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(alumno.id()));
    }

    @Test
    void preceptor_buscaAlumnoPorLegajoDniYApellido() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        String preceptor = tokenPara("30222333", "Preceptor1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin, "ApellidoUnicoBusqueda");

        mockMvc.perform(get("/api/v1/alumnos").param("dni", "30333444")
                        .header("Authorization", "Bearer " + preceptor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dni").value("30333444"));

        mockMvc.perform(get("/api/v1/alumnos").param("apellido", "nicoBusqueda")
                        .header("Authorization", "Bearer " + preceptor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dni").value(alumno.dni()));
    }

    @Test
    void alumno_veSuHistorialYMateriasAprobadasPendientes_despuesDeUnOverrideAdmin() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        AlumnoDeTest alumno = crearAlumnoDeTest(admin);
        long materiaAprobada = crearMateria(admin, "HIST-01");
        long materiaCursando = crearMateria(admin, "HIST-02");

        mockMvc.perform(put("/api/v1/alumnos/" + alumno.id() + "/estado-academico/" + materiaAprobada)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EstadoAcademicoRequest(EstadoMateria.APROBADA, null))))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/alumnos/" + alumno.id() + "/estado-academico/" + materiaCursando)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EstadoAcademicoRequest(EstadoMateria.CURSANDO, null))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/alumnos/me/historial-academico").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/v1/alumnos/me/materias-aprobadas").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].materiaCodigo").value("HIST-01"));

        mockMvc.perform(get("/api/v1/alumnos/me/materias-pendientes").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].materiaCodigo").value("HIST-02"));

        mockMvc.perform(get("/api/v1/alumnos/" + alumno.id() + "/historial-academico")
                        .header("Authorization", "Bearer " + tokenPara("30222333", "Preceptor1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
