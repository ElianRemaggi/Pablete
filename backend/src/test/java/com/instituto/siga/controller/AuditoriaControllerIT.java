package com.instituto.siga.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituto.siga.dto.request.CarreraRequest;
import com.instituto.siga.dto.request.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ejercita el circuito real: Service publica AuditoriaEvent -> AuditoriaListener (@Async,
 * AFTER_COMMIT) -> persistencia -> GET /auditoria. Como la escritura es asincrona y ocurre
 * despues de que la respuesta HTTP ya se envio, se hace polling acotado en vez de asumir
 * que ya esta escrito apenas vuelve el request que dispara la accion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuditoriaControllerIT {

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

    private JsonNode esperarRegistro(String admin, String accion, String entidad) throws Exception {
        for (int intento = 0; intento < 20; intento++) {
            String body = mockMvc.perform(get("/api/v1/auditoria")
                            .param("accion", accion)
                            .param("entidad", entidad)
                            .header("Authorization", "Bearer " + admin))
                    .andReturn().getResponse().getContentAsString();
            JsonNode content = objectMapper.readTree(body).get("content");
            if (content != null && !content.isEmpty()) {
                return content.get(0);
            }
            Thread.sleep(150);
        }
        throw new AssertionError("No aparecio ningun registro de auditoria para " + accion + "/" + entidad + " tras esperar");
    }

    @Test
    void login_generaUnRegistroDeAuditoriaLoginExitoso() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");

        JsonNode registro = esperarRegistro(admin, "LOGIN_EXITOSO", "Usuario");

        assertThat(registro.get("accion").asText()).isEqualTo("LOGIN_EXITOSO");
        assertThat(registro.get("ip").asText()).isNotBlank();
    }

    @Test
    void loginFallido_generaRegistroSinUsuarioResuelto() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("30111222", "incorrecta"))))
                .andExpect(status().isUnauthorized());

        JsonNode registro = esperarRegistro(admin, "LOGIN_FALLIDO", "Usuario");

        JsonNode usuarioId = registro.get("usuarioId");
        assertThat(usuarioId == null || usuarioId.isNull()).isTrue();
    }

    @Test
    void crearCarrera_generaRegistroDeAuditoriaConEntidadId() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        String body = mockMvc.perform(post("/api/v1/carreras")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CarreraRequest("AUD-IT1", "Carrera Auditada"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long carreraId = objectMapper.readTree(body).get("id").asLong();

        for (int intento = 0; intento < 20; intento++) {
            String auditoriaBody = mockMvc.perform(get("/api/v1/auditoria")
                            .param("accion", "CARRERA_CREADA")
                            .header("Authorization", "Bearer " + admin))
                    .andReturn().getResponse().getContentAsString();
            JsonNode content = objectMapper.readTree(auditoriaBody).get("content");
            boolean encontrado = false;
            for (JsonNode nodo : content) {
                if (nodo.get("entidadId").asLong() == carreraId) {
                    encontrado = true;
                    assertThat(nodo.get("detalle").asText()).contains("AUD-IT1");
                    break;
                }
            }
            if (encontrado) {
                return;
            }
            Thread.sleep(150);
        }
        throw new AssertionError("No aparecio el registro CARRERA_CREADA para la carrera " + carreraId);
    }

    @Test
    void alumno_noPuedeConsultarAuditoria_devuelve403() throws Exception {
        String alumno = tokenPara("30333444", "Alumno1234");

        mockMvc.perform(get("/api/v1/auditoria").header("Authorization", "Bearer " + alumno))
                .andExpect(status().isForbidden());
    }
}
