package com.instituto.siga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituto.siga.dto.request.ConfiguracionInstitucionalRequest;
import com.instituto.siga.dto.request.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET abierto a cualquier autenticado, PUT solo ADMIN (docs/04 SS13). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ConfiguracionInstitucionalControllerIT {

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
    void cualquierAutenticado_puedeConsultarLaConfiguracionYaSeeded() throws Exception {
        String alumno = tokenPara("30333444", "Alumno1234");

        mockMvc.perform(get("/api/v1/configuracion").header("Authorization", "Bearer " + alumno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limiteIntentosFinal").value(3))
                .andExpect(jsonPath("$.horasBloqueoFinal").value(48));
    }

    @Test
    void alumno_noPuedeActualizarConfiguracion_devuelve403() throws Exception {
        String alumno = tokenPara("30333444", "Alumno1234");
        LocalDateTime ahora = LocalDateTime.now();
        ConfiguracionInstitucionalRequest request = new ConfiguracionInstitucionalRequest(
                ahora, ahora.plusDays(30), ahora, ahora.plusDays(60), 3, 48);

        mockMvc.perform(put("/api/v1/configuracion")
                        .header("Authorization", "Bearer " + alumno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_actualizaConfiguracion_yConFechasInvalidasDevuelve422() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        LocalDateTime ahora = LocalDateTime.now();

        ConfiguracionInstitucionalRequest valido = new ConfiguracionInstitucionalRequest(
                ahora, ahora.plusDays(45), ahora, ahora.plusDays(90), 4, 72);
        mockMvc.perform(put("/api/v1/configuracion")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(valido)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limiteIntentosFinal").value(4));

        ConfiguracionInstitucionalRequest invalido = new ConfiguracionInstitucionalRequest(
                ahora, ahora.minusDays(1), ahora, ahora.plusDays(90), 4, 72);
        mockMvc.perform(put("/api/v1/configuracion")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("CONFIGURACION_FECHAS_INVALIDAS"));
    }
}
