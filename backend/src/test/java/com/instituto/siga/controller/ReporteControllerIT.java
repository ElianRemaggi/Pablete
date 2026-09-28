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

/** Cubre el reporte de tasa de aprobacion por materia (agregado nuevo, no documentado originalmente en docs/04). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ReporteControllerIT {

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

    private long crearAlumnoDeTest(String admin) throws Exception {
        String dni = "3099" + String.format("%06d", CONTADOR_DNI.incrementAndGet());
        String body = mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UsuarioRequest(dni, dni + "@x.com", "N", "A", "Abcd1234", Set.of("ALUMNO")))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
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
    void alumno_noPuedeVerElReporte_devuelve403() throws Exception {
        String alumno = tokenPara("30333444", "Alumno1234");

        mockMvc.perform(get("/api/v1/reportes/tasa-aprobacion-por-materia").header("Authorization", "Bearer " + alumno))
                .andExpect(status().isForbidden());
    }

    @Test
    void preceptorYAdmin_venLaTasaDeAprobacionPorMateria() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        String preceptor = tokenPara("30222333", "Preceptor1234");
        long alumnoId = crearAlumnoDeTest(admin);
        long materiaId = crearMateria(admin, "REP-01");

        mockMvc.perform(put("/api/v1/alumnos/" + alumnoId + "/estado-academico/" + materiaId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EstadoAcademicoRequest(EstadoMateria.APROBADA, null))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reportes/tasa-aprobacion-por-materia").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.materiaId == " + materiaId + ")].aprobadas").value(1));

        mockMvc.perform(get("/api/v1/reportes/tasa-aprobacion-por-materia").header("Authorization", "Bearer " + preceptor))
                .andExpect(status().isOk());
    }
}
