package com.instituto.siga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Recuperacion de clave: solo el administrador crea una clave nueva para un usuario (PUT /usuarios/{id}/clave). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class NuevaClaveIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private int login(String dni, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(dni, password)))).andReturn().getResponse().getStatus();
    }

    private String token(String dni, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(dni, password)))).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private int resetear(String token, long id, String clave) throws Exception {
        return mockMvc.perform(put("/api/v1/usuarios/" + id + "/clave").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nuevaClave\":\"" + clave + "\"}")).andReturn().getResponse().getStatus();
    }

    @Test
    void elAdministradorCreaUnaClaveNueva_laViejaDejaDeFuncionar() throws Exception {
        String admin = token("30111222", "Admin1234");
        String dni = "3098" + String.format("%06d", System.nanoTime() % 1000000);
        String body = mockMvc.perform(post("/api/v1/usuarios").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UsuarioRequest(dni, dni + "@x.com", "Olvido", "Clave", "Vieja1234", Set.of("ALUMNO")))))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(body).get("id").asLong();
        assertThat(login(dni, "Vieja1234")).isEqualTo(200);

        assertThat(resetear(admin, id, "Nueva5678")).isEqualTo(204);

        assertThat(login(dni, "Vieja1234")).isEqualTo(401);
        assertThat(login(dni, "Nueva5678")).isEqualTo(200);
    }

    @Test
    void validaLaClave_yNegaElPermisoANoAdministradores() throws Exception {
        String admin = token("30111222", "Admin1234");
        assertThat(resetear(admin, 3, "corta")).isEqualTo(400);
        assertThat(resetear(admin, 99999999L, "Nueva5678")).isEqualTo(404);
        assertThat(resetear(token("30333444", "Alumno1234"), 3, "Nueva5678")).isEqualTo(403);
        assertThat(resetear(token("30222333", "Preceptor1234"), 3, "Nueva5678")).isEqualTo(403);
        assertThat(login("30333444", "Alumno1234")).isEqualTo(200); // la clave del alumno demo no cambio
    }
}
