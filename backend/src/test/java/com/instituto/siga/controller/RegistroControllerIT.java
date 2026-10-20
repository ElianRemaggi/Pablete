package com.instituto.siga.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Autoregistro de alumnos, login con la cuenta creada y edicion del propio email. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class RegistroControllerIT {

    private static final AtomicInteger CONTADOR = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String dniNuevo() {
        return "4001" + String.format("%04d", CONTADOR.incrementAndGet());
    }

    private ResultActions registrar(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String json(String dni, String email, String password) {
        return "{\"dni\":\"" + dni + "\",\"email\":\"" + email + "\",\"nombre\":\"Ana\",\"apellido\":\"Gomez\",\"password\":\"" + password + "\"}";
    }

    private JsonNode login(String dni, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"" + dni + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    @Test
    void registroValido_creaAlumno_yPermiteIniciarSesionSinNecesitarToken() throws Exception {
        String dni = dniNuevo();
        registrar(json(dni, dni + "@mail.com", "Clave1234"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dni").value(dni))
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("ALUMNO"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        JsonNode sesion = login(dni, "Clave1234");
        assertThat(sesion.get("rol").asText()).isEqualTo("ALUMNO");

        // Existe como Alumno (legajo generado): puede consultar sus datos academicos
        mockMvc.perform(get("/api/v1/alumnos/me").header("Authorization", "Bearer " + sesion.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value(dni))
                .andExpect(jsonPath("$.legajo").isNotEmpty());
    }

    @Test
    void registro_ignoraCualquierIntentoDeAsignarseOtroRol() throws Exception {
        String dni = dniNuevo();
        registrar("{\"dni\":\"" + dni + "\",\"email\":\"" + dni + "@mail.com\",\"nombre\":\"Eve\",\"apellido\":\"Hacker\","
                + "\"password\":\"Clave1234\",\"roles\":[\"ADMINISTRADOR\"],\"rol\":\"ADMINISTRADOR\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("ALUMNO"));

        assertThat(login(dni, "Clave1234").get("rol").asText()).isEqualTo("ALUMNO");
        // Y por lo tanto no accede a operaciones administrativas
        String token = login(dni, "Clave1234").get("accessToken").asText();
        mockMvc.perform(get("/api/v1/usuarios").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test
    void registro_conDniOEmailDuplicados_devuelve409() throws Exception {
        String dni = dniNuevo();
        registrar(json(dni, dni + "@mail.com", "Clave1234")).andExpect(status().isCreated());

        registrar(json(dni, "otro" + dni + "@mail.com", "Clave1234"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DNI_DUPLICADO"));
        registrar(json(dniNuevo(), dni + "@mail.com", "Clave1234"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EMAIL_DUPLICADO"));
        // Un DNI de un usuario sembrado (administrador) tampoco se puede registrar
        registrar(json("30111222", "nuevo@mail.com", "Clave1234"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DNI_DUPLICADO"));
    }

    @Test
    void registro_conDatosInvalidos_devuelve400() throws Exception {
        String dni = dniNuevo();
        registrar(json("12.345.678", dni + "@mail.com", "Clave1234")).andExpect(status().isBadRequest());
        registrar(json("123", dni + "@mail.com", "Clave1234")).andExpect(status().isBadRequest());
        registrar(json(dni, "no-es-un-mail", "Clave1234")).andExpect(status().isBadRequest());
        registrar(json(dni, dni + "@mail.com", "corta")).andExpect(status().isBadRequest());
        registrar(json(dni, dni + "@mail.com", "sinmayuscula123")).andExpect(status().isBadRequest());
        registrar("{\"dni\":\"" + dni + "\",\"email\":\"" + dni + "@mail.com\",\"password\":\"Clave1234\"}")
                .andExpect(status().isBadRequest());
        // Nada de lo anterior debe haber creado la cuenta
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"" + dni + "\",\"password\":\"Clave1234\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_conPasswordIncorrecta_devuelve401_yUnaRutaProtegidaSinTokenTambien() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"30333444\",\"password\":\"Incorrecta1\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/alumnos/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void perfil_elAlumnoSoloPuedeCambiarSuEmail_yNoElDeOtro() throws Exception {
        String dni = dniNuevo();
        String otroDni = dniNuevo();
        registrar(json(dni, dni + "@mail.com", "Clave1234")).andExpect(status().isCreated());
        registrar(json(otroDni, otroDni + "@mail.com", "Clave1234")).andExpect(status().isCreated());
        String token = login(dni, "Clave1234").get("accessToken").asText();

        mockMvc.perform(put("/api/v1/usuarios/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"nuevo" + dni + "@mail.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("nuevo" + dni + "@mail.com"))
                .andExpect(jsonPath("$.dni").value(dni));
        // email ya usado por otro
        mockMvc.perform(put("/api/v1/usuarios/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + otroDni + "@mail.com\"}"))
                .andExpect(status().isConflict());
        // email invalido y sin token
        mockMvc.perform(put("/api/v1/usuarios/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"roto\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/usuarios/me").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@b.com\"}"))
                .andExpect(status().isUnauthorized());
        // Escalada: el alumno no puede usar el PUT administrativo sobre otro usuario
        mockMvc.perform(put("/api/v1/usuarios/1").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"x\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/usuarios/1/roles").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"ADMINISTRADOR\"]}"))
                .andExpect(status().isForbidden());
    }
}
