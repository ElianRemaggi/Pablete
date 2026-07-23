package com.instituto.siga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituto.siga.dto.request.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cubre Certificados PDF (docs/04 SS11) con JWT reales. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CertificadoControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String tokenPara(String dni, String password) throws Exception {
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(dni, password))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    void alumno_descargaCertificadoDeAlumnoRegular_devuelvePdfValido() throws Exception {
        String alumno = tokenPara("30333444", "Alumno1234");

        byte[] pdf = mockMvc.perform(get("/api/v1/certificados/alumno-regular").header("Authorization", "Bearer " + alumno))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("application/pdf")))
                .andReturn().getResponse().getContentAsByteArray();

        assertPdfValido(pdf);
    }

    @Test
    void alumno_descargaCertificadoAnaliticoParcial_devuelvePdfValido() throws Exception {
        String alumno = tokenPara("30333444", "Alumno1234");

        byte[] pdf = mockMvc.perform(get("/api/v1/certificados/analitico-parcial").header("Authorization", "Bearer " + alumno))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("application/pdf")))
                .andReturn().getResponse().getContentAsByteArray();

        assertPdfValido(pdf);
    }

    @Test
    void noAlumno_noPuedeDescargarCertificadoPropio() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");

        mockMvc.perform(get("/api/v1/certificados/alumno-regular").header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());
    }

    @Test
    void preceptor_emiteCertificadoParaUnAlumnoPuntual() throws Exception {
        String preceptor = tokenPara("30222333", "Preceptor1234");

        byte[] pdf = mockMvc.perform(get("/api/v1/certificados/alumno/3/alumno-regular")
                        .header("Authorization", "Bearer " + preceptor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertPdfValido(pdf);
    }

    @Test
    void alumno_veSuHistorialDeCertificadosDespuesDeDescargar() throws Exception {
        String alumno = tokenPara("30333444", "Alumno1234");

        mockMvc.perform(get("/api/v1/certificados/alumno-regular").header("Authorization", "Bearer " + alumno))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/certificados/historial").header("Authorization", "Bearer " + alumno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").exists())
                .andExpect(jsonPath("$[0].urlDescarga").value("/api/v1/certificados/alumno-regular"));
    }

    private void assertPdfValido(byte[] pdf) {
        org.assertj.core.api.Assertions.assertThat(pdf).isNotEmpty();
        org.assertj.core.api.Assertions.assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1))
                .isEqualTo("%PDF-");
    }
}
