package com.instituto.siga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituto.siga.dto.request.LoginRequest;
import com.instituto.siga.dto.request.UsuarioRequest;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Constancia de alumno regular: formulario EN BLANCO con el formato de "ALUMNO REGULAR ISET.pdf" (se completa a mano).
 * Disponible solo para alumnos (la propia) y para el personal autorizado. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ConstanciaAlumnoRegularIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String token(String dni, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(dni, password))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private String textoDelPdf(byte[] pdf) throws Exception {
        PdfReader lector = new PdfReader(pdf);
        assertThat(lector.getNumberOfPages()).isEqualTo(1);
        return new PdfTextExtractor(lector).getTextFromPage(1).replaceAll("\\s+", " ");
    }

    @Test
    void alumno_descargaElFormularioEnBlancoEnPdf() throws Exception {
        MvcResult r = mockMvc.perform(get("/api/v1/certificados/alumno-regular")
                .header("Authorization", "Bearer " + token("30333444", "Alumno1234"))).andReturn();

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat(r.getResponse().getContentType()).contains("application/pdf");
        byte[] pdf = r.getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        Files.write(Path.of("target", "constancia-alumno-regular-ejemplo.pdf"), pdf); // para inspeccion visual
        String texto = textoDelPdf(pdf);
        assertThat(texto).contains("CONSTANCIA DE ALUMNO REGULAR").contains("Se deja constancia que")
                .contains("Instituto Superior de Estudios Técnicos").contains("Mar del Plata").contains("....");
        assertThat(texto).doesNotContain("30333444"); // en blanco: sin datos del alumno
    }

    @Test
    void cualquierAlumnoPuedeDescargarloSinRequisitosDeRegularidad() throws Exception {
        String admin = token("30111222", "Admin1234");
        String dni = "3097" + String.format("%06d", System.nanoTime() % 1000000);
        mockMvc.perform(post("/api/v1/usuarios").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new UsuarioRequest(dni, dni + "@x.com", "Nuevo", "Alumno", "Abcd1234", Set.of("ALUMNO")))));

        MvcResult r = mockMvc.perform(get("/api/v1/certificados/alumno-regular")
                .header("Authorization", "Bearer " + token(dni, "Abcd1234"))).andReturn();

        assertThat(r.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void permisos_unAlumnoNoUsaLaRutaDelPersonal_yElPersonalSi() throws Exception {
        String alumno = token("30333444", "Alumno1234");
        String preceptor = token("30222333", "Preceptor1234");
        long alumnoId = objectMapper.readTree(mockMvc.perform(get("/api/v1/alumnos/me")
                .header("Authorization", "Bearer " + alumno)).andReturn().getResponse().getContentAsString())
                .get("usuarioId").asLong();

        assertThat(mockMvc.perform(get("/api/v1/certificados/alumno/" + alumnoId + "/alumno-regular")
                .header("Authorization", "Bearer " + alumno)).andReturn().getResponse().getStatus()).isEqualTo(403);
        assertThat(mockMvc.perform(get("/api/v1/certificados/alumno-regular")).andReturn().getResponse().getStatus())
                .isEqualTo(401);

        MvcResult delPersonal = mockMvc.perform(get("/api/v1/certificados/alumno/" + alumnoId + "/alumno-regular")
                .header("Authorization", "Bearer " + preceptor)).andReturn();
        assertThat(delPersonal.getResponse().getStatus()).isEqualTo(200);
        assertThat(textoDelPdf(delPersonal.getResponse().getContentAsByteArray())).contains("CONSTANCIA DE ALUMNO REGULAR");
    }
}
