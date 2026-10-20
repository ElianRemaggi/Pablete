package com.instituto.siga.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituto.siga.dto.request.ComisionRequest;
import com.instituto.siga.dto.request.HorarioRequest;
import com.instituto.siga.dto.request.LoginRequest;
import com.instituto.siga.dto.request.MateriaRequest;
import com.instituto.siga.dto.request.MesaExamenRequest;
import com.instituto.siga.entity.Turno;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cubre Comision/Horario y MesaExamen: roles y las validaciones de docs/07 SS4.3/SS4.4 con JWT reales. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ComisionMesaExamenControllerIT {

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

    private Long crearMateria(String admin, String codigo) throws Exception {
        String body = mockMvc.perform(post("/api/v1/materias")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MateriaRequest(codigo, "Materia " + codigo, 100))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    void alumno_noPuedeCrearComision_devuelve403() throws Exception {
        String alumno = tokenPara("30333444", "Alumno1234");
        ComisionRequest request = new ComisionRequest(1L, "A", "2026-1C", 40, null, List.of());

        mockMvc.perform(post("/api/v1/comisiones")
                        .header("Authorization", "Bearer " + alumno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_creaComisionConHorarios_yDuplicadaDevuelve409() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        Long materiaId = crearMateria(admin, "MAT-CIT1");
        HorarioRequest horario = new HorarioRequest(1, LocalTime.of(18, 0), LocalTime.of(22, 0), "Aula 3");
        ComisionRequest request = new ComisionRequest(materiaId, "A", "2026-1C", 40, "Prof. Test", List.of(horario));

        mockMvc.perform(post("/api/v1/comisiones")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horarios[0].aula").value("Aula 3"));

        mockMvc.perform(post("/api/v1/comisiones")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("COMISION_DUPLICADA"));
    }

    @Test
    void admin_horariosSuperpuestosEnAlta_devuelve422() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        Long materiaId = crearMateria(admin, "MAT-CIT2");
        HorarioRequest h1 = new HorarioRequest(2, LocalTime.of(18, 0), LocalTime.of(22, 0), "Aula 1");
        HorarioRequest h2 = new HorarioRequest(2, LocalTime.of(19, 0), LocalTime.of(20, 0), "Aula 2");
        ComisionRequest request = new ComisionRequest(materiaId, "B", "2026-1C", 40, null, List.of(h1, h2));

        mockMvc.perform(post("/api/v1/comisiones")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("HORARIO_SUPERPUESTO"));
    }

    @Test
    void admin_mesaConFechaPasada_devuelve422() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        Long materiaId = crearMateria(admin, "MAT-CIT3");
        MesaExamenRequest request = new MesaExamenRequest(materiaId, LocalDate.now().minusDays(1), Turno.MANANA, 20, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/mesas-examen")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("MESA_FUERA_DE_FECHA"));
    }

    @Test
    void admin_mesaDuplicada_devuelve409() throws Exception {
        String admin = tokenPara("30111222", "Admin1234");
        Long materiaId = crearMateria(admin, "MAT-CIT4");
        MesaExamenRequest request = new MesaExamenRequest(materiaId, LocalDate.now().plusDays(60), Turno.TARDE, 20, null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/mesas-examen")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/mesas-examen")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("MESA_DUPLICADA"));
    }
}
