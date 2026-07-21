package com.instituto.siga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituto.siga.dto.request.CarreraRequest;
import com.instituto.siga.dto.request.LoginRequest;
import com.instituto.siga.dto.request.UsuarioRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cubre el enforcement de roles (@PreAuthorize) del ABM de usuarios y del modulo Carrera, con JWT reales. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class UsuarioCarreraControllerIT {

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

    @Test
    void admin_puedeCrearCarrera_yLuegoListarla() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");

        mockMvc.perform(post("/api/v1/carreras")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CarreraRequest("TSW-IT1", "Carrera de Test 1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(true));

        mockMvc.perform(get("/api/v1/carreras").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
    }

    @Test
    void alumno_noPuedeCrearCarrera_devuelve403() throws Exception {
        String alumno = tokenPara("30333444", "Alumno1234");

        mockMvc.perform(post("/api/v1/carreras")
                        .header("Authorization", "Bearer " + alumno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CarreraRequest("TSW-IT2", "Carrera de Test 2"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void cualquierAutenticado_puedeListarCarreras() throws Exception {
        String alumno = tokenPara("30333444", "Alumno1234");

        mockMvc.perform(get("/api/v1/carreras").header("Authorization", "Bearer " + alumno))
                .andExpect(status().isOk());
    }

    @Test
    void admin_puedeCrearUsuario_yDniDuplicadoDevuelve409() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        UsuarioRequest nuevo = new UsuarioRequest("30555666", "nuevo.it@instituto.edu.ar", "Nuevo", "Test",
                "Abcd1234", Set.of("ALUMNO"));

        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ALUMNO"));

        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevo)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DNI_DUPLICADO"));
    }

    @Test
    void alumno_puedeVerSuPropioUsuario_peroNoListarATodos() throws Exception {
        String alumno = tokenPara("30333444", "Alumno1234");

        mockMvc.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + alumno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value("30333444"));

        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + alumno))
                .andExpect(status().isForbidden());
    }
}
