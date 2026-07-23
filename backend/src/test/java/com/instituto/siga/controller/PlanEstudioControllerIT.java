package com.instituto.siga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituto.siga.dto.request.CarreraRequest;
import com.instituto.siga.dto.request.LoginRequest;
import com.instituto.siga.dto.request.MateriaRequest;
import com.instituto.siga.dto.request.PlanEstudioRequest;
import com.instituto.siga.dto.request.PlanMateriaRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cubre Plan de Estudio (alta/actualizacion/listado) y asociacion Materia-Plan con JWT reales (docs/07 SS4.2). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PlanEstudioControllerIT {

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

    private Long crearCarrera(String admin, String codigo, String nombre) throws Exception {
        String body = mockMvc.perform(post("/api/v1/carreras")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CarreraRequest(codigo, nombre))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
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

    private Long crearPlan(String admin, Long carreraId, String version) throws Exception {
        String body = mockMvc.perform(post("/api/v1/carreras/" + carreraId + "/planes")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlanEstudioRequest(version, true))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    void alumno_noPuedeCrearPlan_devuelve403() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        String alumno = tokenPara("30333444", "Alumno1234");
        Long carreraId = crearCarrera(admin, "PLAN-IT1", "Tec. en Sistemas IT1");

        mockMvc.perform(post("/api/v1/carreras/" + carreraId + "/planes")
                        .header("Authorization", "Bearer " + alumno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlanEstudioRequest("2024", true))))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_creaPlan_yLaVersionDuplicadaParaLaMismaCarreraDevuelve409() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        Long carreraId = crearCarrera(admin, "PLAN-IT2", "Tec. en Sistemas IT2");

        mockMvc.perform(post("/api/v1/carreras/" + carreraId + "/planes")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlanEstudioRequest("2024", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("2024"))
                .andExpect(jsonPath("$.vigente").value(true));

        mockMvc.perform(post("/api/v1/carreras/" + carreraId + "/planes")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlanEstudioRequest("2024", true))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PLAN_ESTUDIO_DUPLICADO"));
    }

    @Test
    void cualquierAutenticado_puedeListarPlanesDeUnaCarrera() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        String alumno = tokenPara("30333444", "Alumno1234");
        Long carreraId = crearCarrera(admin, "PLAN-IT3", "Tec. en Sistemas IT3");
        crearPlan(admin, carreraId, "2024");

        mockMvc.perform(get("/api/v1/carreras/" + carreraId + "/planes")
                        .header("Authorization", "Bearer " + alumno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value("2024"));
    }

    @Test
    void admin_actualizaPlan_yConVersionDuplicadaDeOtroPlanDevuelve409() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        Long carreraId = crearCarrera(admin, "PLAN-IT4", "Tec. en Sistemas IT4");
        Long plan2023 = crearPlan(admin, carreraId, "2023");
        Long plan2024 = crearPlan(admin, carreraId, "2024");

        mockMvc.perform(put("/api/v1/planes-estudio/" + plan2023)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlanEstudioRequest("2023-B", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("2023-B"))
                .andExpect(jsonPath("$.vigente").value(false));

        mockMvc.perform(put("/api/v1/planes-estudio/" + plan2024)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlanEstudioRequest("2023-B", true))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PLAN_ESTUDIO_DUPLICADO"));
    }

    @Test
    void admin_asociaMateriaAPlan_yLaAsociacionDuplicadaDevuelve409() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        Long carreraId = crearCarrera(admin, "PLAN-IT5", "Tec. en Sistemas IT5");
        Long planId = crearPlan(admin, carreraId, "2024");
        Long materiaId = crearMateria(admin, "MAT-PLAN1", "Materia Plan 1");

        mockMvc.perform(post("/api/v1/materias/" + materiaId + "/plan/" + planId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlanMateriaRequest(1, 1))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/materias/" + materiaId + "/plan/" + planId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlanMateriaRequest(1, 1))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MATERIA_YA_ASOCIADA_AL_PLAN"));
    }

    @Test
    void listarMaterias_filtraPorCarreraPlanYAnio() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        Long carreraId = crearCarrera(admin, "PLAN-IT6", "Tec. en Sistemas IT6");
        Long planId = crearPlan(admin, carreraId, "2024");
        Long materiaAsociada = crearMateria(admin, "MAT-PLAN2", "Materia Plan 2");
        crearMateria(admin, "MAT-PLAN3", "Materia Sin Plan");

        mockMvc.perform(post("/api/v1/materias/" + materiaAsociada + "/plan/" + planId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlanMateriaRequest(1, 1))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/materias")
                        .header("Authorization", "Bearer " + admin)
                        .param("carreraId", String.valueOf(carreraId))
                        .param("planId", String.valueOf(planId))
                        .param("anio", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("MAT-PLAN2"));
    }
}
