package com.instituto.siga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituto.siga.dto.request.CorrelatividadRequest;
import com.instituto.siga.dto.request.LoginRequest;
import com.instituto.siga.dto.request.MateriaRequest;
import com.instituto.siga.entity.TipoRequisito;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cubre Materia CRUD, roles, y la deteccion de ciclos de Correlatividad con JWT reales (docs/07 SS4.2). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class MateriaCorrelatividadControllerIT {

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

    private Long crearMateria(String admin, String codigo, String nombre) throws Exception {
        String body = mockMvc.perform(post("/api/v1/materias")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MateriaRequest(codigo, nombre, 100))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    void alumno_noPuedeCrearMateria_devuelve403() throws Exception {
        String alumno = tokenPara("30333444", "Alumno1234");

        mockMvc.perform(post("/api/v1/materias")
                        .header("Authorization", "Bearer " + alumno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MateriaRequest("MAT-IT1", "Materia Test", 80))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cualquierAutenticado_puedeListarMaterias() throws Exception {
        String alumno = tokenPara("30333444", "Alumno1234");

        mockMvc.perform(get("/api/v1/materias").header("Authorization", "Bearer " + alumno))
                .andExpect(status().isOk());
    }

    @Test
    void admin_creaCorrelatividad_yLaDuplicadaDevuelve409() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        Long m1 = crearMateria(admin, "MAT-IT2", "Materia A");
        Long m2 = crearMateria(admin, "MAT-IT3", "Materia B");

        mockMvc.perform(post("/api/v1/correlatividades")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CorrelatividadRequest(m1, m2, TipoRequisito.APROBAR))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoRequisito").value("APROBAR"));

        mockMvc.perform(post("/api/v1/correlatividades")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CorrelatividadRequest(m1, m2, TipoRequisito.APROBAR))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORRELATIVIDAD_DUPLICADA"));
    }

    @Test
    void admin_correlatividadQueCierraUnCiclo_devuelve422() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        Long a = crearMateria(admin, "MAT-IT4", "Materia Ciclo A");
        Long b = crearMateria(admin, "MAT-IT5", "Materia Ciclo B");

        // A requiere B.
        mockMvc.perform(post("/api/v1/correlatividades")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CorrelatividadRequest(a, b, TipoRequisito.CURSAR))))
                .andExpect(status().isOk());

        // B requiere A -> cerraria el ciclo A -> B -> A.
        mockMvc.perform(post("/api/v1/correlatividades")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CorrelatividadRequest(b, a, TipoRequisito.CURSAR))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CORRELATIVIDAD_CICLICA"));
    }

    @Test
    void admin_correlatividadAutoreferencial_devuelve422() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        Long m = crearMateria(admin, "MAT-IT6", "Materia Auto");

        mockMvc.perform(post("/api/v1/correlatividades")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CorrelatividadRequest(m, m, TipoRequisito.CURSAR))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CORRELATIVIDAD_AUTOREFERENCIA"));
    }
}
