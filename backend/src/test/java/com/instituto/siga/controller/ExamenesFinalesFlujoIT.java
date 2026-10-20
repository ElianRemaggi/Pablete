package com.instituto.siga.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituto.siga.dto.request.EstadoAcademicoRequest;
import com.instituto.siga.dto.request.LoginRequest;
import com.instituto.siga.dto.request.MateriaRequest;
import com.instituto.siga.dto.request.MesaExamenRequest;
import com.instituto.siga.dto.request.UsuarioRequest;
import com.instituto.siga.entity.EstadoMateria;
import com.instituto.siga.entity.Modalidad;
import com.instituto.siga.entity.Turno;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Circuito completo de la primera entrega (examenes finales) con JWT reales: ciclo de vida de la mesa,
 * permisos por rol, inscripcion con sus reglas (publicada, plazo, cupo, duplicados, regularidad),
 * propiedad de las inscripciones, comprobante PDF y cancelacion de mesa conservando el historial.
 * Cada test arma sus propias materias/alumnos (contexto compartido, sin rollback entre metodos).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ExamenesFinalesFlujoIT {

    private static final AtomicInteger CONTADOR = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    // ---------------------------------------------------------------- helpers

    private String tokenPara(String dni, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(dni, password))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private String admin() throws Exception {
        return tokenPara("30111222", "Admin1234");
    }

    private record Alumno(long id, String token) {
    }

    private Alumno crearAlumno(String admin) throws Exception {
        String dni = "3093" + String.format("%06d", CONTADOR.incrementAndGet());
        String body = mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UsuarioRequest(dni, dni + "@x.com", "Nombre", "Apellido", "Abcd1234", Set.of("ALUMNO")))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new Alumno(objectMapper.readTree(body).get("id").asLong(), tokenPara(dni, "Abcd1234"));
    }

    private long crearMateria(String admin) throws Exception {
        String codigo = "EF-" + CONTADOR.incrementAndGet();
        String body = mockMvc.perform(post("/api/v1/materias")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MateriaRequest(codigo, "Materia " + codigo, 100))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private void fijarEstado(String admin, long alumnoId, long materiaId, EstadoMateria estado) throws Exception {
        mockMvc.perform(put("/api/v1/alumnos/" + alumnoId + "/estado-academico/" + materiaId)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EstadoAcademicoRequest(estado, null))))
                .andExpect(status().isOk());
    }

    private MesaExamenRequest requestMesa(long materiaId, int cupo, LocalDateTime apertura, LocalDateTime cierre) {
        return new MesaExamenRequest(materiaId, LocalDate.now().plusDays(30 + CONTADOR.incrementAndGet() % 300), Turno.MANANA,
                cupo, "Prof. Test", LocalTime.of(9, 0), "2026-2", "Aula 7", Modalidad.PRESENCIAL, apertura, cierre);
    }

    /** Crea la mesa (queda en BORRADOR) y devuelve su id. */
    private long crearMesaBorrador(String admin, MesaExamenRequest request) throws Exception {
        String body = mockMvc.perform(post("/api/v1/mesas-examen")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private void cambiarEstadoMesa(String admin, long mesaId, String accion) throws Exception {
        mockMvc.perform(patch("/api/v1/mesas-examen/" + mesaId + "/" + accion).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
    }

    private long crearMesaPublicada(String admin, long materiaId, int cupo) throws Exception {
        long id = crearMesaBorrador(admin, requestMesa(materiaId, cupo, null, null));
        cambiarEstadoMesa(admin, id, "publicar");
        return id;
    }

    private MvcResultHolder inscribir(Alumno alumno, long mesaId) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/inscripciones-final")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mesaId\":" + mesaId + "}"))
                .andReturn();
        return new MvcResultHolder(resultado.getResponse().getStatus(), resultado.getResponse().getContentAsString());
    }

    private record MvcResultHolder(int status, String body) {
        String codigo(ObjectMapper mapper) throws Exception {
            return mapper.readTree(body).path("codigo").asText();
        }
    }

    // ------------------------------------------------------------------ tests

    @Test
    void alumno_noPuedeCrearModificarNiPublicarMesas() throws Exception {
        String admin = admin();
        Alumno alumno = crearAlumno(admin);
        long materia = crearMateria(admin);
        long mesa = crearMesaBorrador(admin, requestMesa(materia, 5, null, null));

        mockMvc.perform(post("/api/v1/mesas-examen")
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestMesa(materia, 5, null, null))))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/mesas-examen/" + mesa)
                        .header("Authorization", "Bearer " + alumno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestMesa(materia, 5, null, null))))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/mesas-examen/" + mesa + "/publicar").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/mesas-examen/" + mesa + "/cancelar").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/mesas-examen/" + mesa).header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinToken_losEndpointsDeFinalesDevuelven401() throws Exception {
        mockMvc.perform(get("/api/v1/mesas-examen")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/inscripciones-final").contentType(MediaType.APPLICATION_JSON).content("{\"mesaId\":1}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/certificados/inscripcion-final/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void mesaEnBorrador_noSeVeNiAdmiteInscripcion_yPublicadaSi() throws Exception {
        String admin = admin();
        Alumno alumno = crearAlumno(admin);
        long materia = crearMateria(admin);
        fijarEstado(admin, alumno.id(), materia, EstadoMateria.REGULAR);
        long mesa = crearMesaBorrador(admin, requestMesa(materia, 5, null, null));

        // El alumno no la ve en el listado ni por id
        String listado = mockMvc.perform(get("/api/v1/mesas-examen").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(listado).doesNotContain("\"id\":" + mesa + ",");
        mockMvc.perform(get("/api/v1/mesas-examen/" + mesa).header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isNotFound());
        // El administrador si
        mockMvc.perform(get("/api/v1/mesas-examen/" + mesa).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("BORRADOR"));

        MvcResultHolder rechazo = inscribir(alumno, mesa);
        assertThat(rechazo.status()).isEqualTo(422);
        assertThat(rechazo.codigo(objectMapper)).isEqualTo("MESA_NO_DISPONIBLE");

        cambiarEstadoMesa(admin, mesa, "publicar");

        mockMvc.perform(get("/api/v1/mesas-examen/" + mesa).header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PUBLICADA"))
                .andExpect(jsonPath("$.inscripcionAbierta").value(true))
                .andExpect(jsonPath("$.cupoDisponible").value(5));
        assertThat(inscribir(alumno, mesa).status()).isEqualTo(200);
    }

    @Test
    void inscripcion_duplicada_devuelve409() throws Exception {
        String admin = admin();
        Alumno alumno = crearAlumno(admin);
        long materia = crearMateria(admin);
        fijarEstado(admin, alumno.id(), materia, EstadoMateria.REGULAR);
        long mesa = crearMesaPublicada(admin, materia, 5);

        assertThat(inscribir(alumno, mesa).status()).isEqualTo(200);
        MvcResultHolder duplicada = inscribir(alumno, mesa);
        assertThat(duplicada.status()).isEqualTo(409);
        assertThat(duplicada.codigo(objectMapper)).isEqualTo("INSCRIPCION_DUPLICADA");
    }

    @Test
    void cupoAgotado_rechazaAlSiguienteAlumno() throws Exception {
        String admin = admin();
        Alumno primero = crearAlumno(admin);
        Alumno segundo = crearAlumno(admin);
        long materia = crearMateria(admin);
        fijarEstado(admin, primero.id(), materia, EstadoMateria.REGULAR);
        fijarEstado(admin, segundo.id(), materia, EstadoMateria.REGULAR);
        long mesa = crearMesaPublicada(admin, materia, 1);

        assertThat(inscribir(primero, mesa).status()).isEqualTo(200);
        MvcResultHolder sinLugar = inscribir(segundo, mesa);
        assertThat(sinLugar.status()).isEqualTo(422);
        assertThat(sinLugar.codigo(objectMapper)).isEqualTo("CUPO_AGOTADO");

        mockMvc.perform(get("/api/v1/mesas-examen/" + mesa).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.inscriptos").value(1))
                .andExpect(jsonPath("$.cupoDisponible").value(0));
    }

    @Test
    void inscripcionesSimultaneas_nuncaSuperanElCupo() throws Exception {
        String admin = admin();
        long materia = crearMateria(admin);
        int cupo = 2;
        int aspirantes = 6;
        List<Alumno> alumnos = new ArrayList<>();
        for (int i = 0; i < aspirantes; i++) {
            Alumno a = crearAlumno(admin);
            fijarEstado(admin, a.id(), materia, EstadoMateria.REGULAR);
            alumnos.add(a);
        }
        long mesa = crearMesaPublicada(admin, materia, cupo);

        ExecutorService pool = Executors.newFixedThreadPool(aspirantes);
        try {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (Alumno a : alumnos) {
                Callable<Integer> tarea = () -> inscribir(a, mesa).status();
                futuros.add(pool.submit(tarea));
            }
            int exitosas = 0;
            for (Future<Integer> f : futuros) {
                if (f.get() == 200) exitosas++;
            }
            assertThat(exitosas).as("inscripciones aceptadas con cupo %d", cupo).isEqualTo(cupo);
        } finally {
            pool.shutdownNow();
        }
        String body = mockMvc.perform(get("/api/v1/inscripciones-final/mesa/" + mesa).header("Authorization", "Bearer " + admin))
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(body).size()).isEqualTo(cupo);
    }

    @Test
    void inscripcion_sinRegularizar_respetaLaValidacionAcademica() throws Exception {
        String admin = admin();
        Alumno alumno = crearAlumno(admin);
        long materia = crearMateria(admin);
        long mesa = crearMesaPublicada(admin, materia, 5);

        MvcResultHolder rechazo = inscribir(alumno, mesa);
        assertThat(rechazo.status()).isEqualTo(422);
        assertThat(rechazo.codigo(objectMapper)).isEqualTo("MATERIA_NO_REGULARIZADA");
    }

    @Test
    void ventanaPropiaDeLaMesa_fueraDePlazoRechaza_yDentroAcepta() throws Exception {
        String admin = admin();
        Alumno alumno = crearAlumno(admin);
        long materia = crearMateria(admin);
        fijarEstado(admin, alumno.id(), materia, EstadoMateria.REGULAR);

        // Ventana que todavia no abrio
        long futura = crearMesaBorrador(admin,
                requestMesa(materia, 5, LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(10)));
        cambiarEstadoMesa(admin, futura, "publicar");
        MvcResultHolder rechazo = inscribir(alumno, futura);
        assertThat(rechazo.status()).isEqualTo(422);
        assertThat(rechazo.codigo(objectMapper)).isEqualTo("FUERA_DE_PLAZO");
        mockMvc.perform(get("/api/v1/mesas-examen/" + futura).header("Authorization", "Bearer " + alumno.token()))
                .andExpect(jsonPath("$.inscripcionAbierta").value(false));

        // Ventana abierta ahora
        long abierta = crearMesaBorrador(admin,
                requestMesa(materia, 5, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(10)));
        cambiarEstadoMesa(admin, abierta, "publicar");
        assertThat(inscribir(alumno, abierta).status()).isEqualTo(200);
    }

    @Test
    void mesaCerrada_noAdmiteInscripciones_yCanceladaDaDeBajaSinBorrarHistorial() throws Exception {
        String admin = admin();
        Alumno alumno = crearAlumno(admin);
        Alumno otro = crearAlumno(admin);
        long materia = crearMateria(admin);
        fijarEstado(admin, alumno.id(), materia, EstadoMateria.REGULAR);
        fijarEstado(admin, otro.id(), materia, EstadoMateria.REGULAR);
        long mesa = crearMesaPublicada(admin, materia, 5);

        String body = inscribir(alumno, mesa).body();
        long inscripcionId = objectMapper.readTree(body).get("id").asLong();

        cambiarEstadoMesa(admin, mesa, "cerrar");
        MvcResultHolder cerrada = inscribir(otro, mesa);
        assertThat(cerrada.status()).isEqualTo(422);
        assertThat(cerrada.codigo(objectMapper)).isEqualTo("MESA_NO_DISPONIBLE");
        // La inscripcion previa se conserva
        mockMvc.perform(get("/api/v1/inscripciones-final/me").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(jsonPath("$[0].estado").value("INSCRIPTO"));

        cambiarEstadoMesa(admin, mesa, "cancelar");

        // Historial intacto: la inscripcion sigue ahi, ahora dada de baja, con la mesa cancelada
        mockMvc.perform(get("/api/v1/inscripciones-final/me").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(inscripcionId))
                .andExpect(jsonPath("$[0].estado").value("BAJA"))
                .andExpect(jsonPath("$[0].mesaEstado").value("CANCELADA"));
        // La mesa no se borro y no se puede reabrir ni borrar
        mockMvc.perform(get("/api/v1/mesas-examen/" + mesa).header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        mockMvc.perform(patch("/api/v1/mesas-examen/" + mesa + "/publicar").header("Authorization", "Bearer " + admin))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(delete("/api/v1/mesas-examen/" + mesa).header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
        // El comprobante refleja el estado real
        MvcResult pdf = mockMvc.perform(get("/api/v1/certificados/inscripcion-final/" + inscripcionId)
                        .header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isOk()).andReturn();
        assertThat(pdf.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    @Test
    void comprobantePdf_esValido_ySoloLoDescargaSuDuenioOElPersonal() throws Exception {
        String admin = admin();
        Alumno alumno = crearAlumno(admin);
        Alumno ajeno = crearAlumno(admin);
        long materia = crearMateria(admin);
        fijarEstado(admin, alumno.id(), materia, EstadoMateria.REGULAR);
        long mesa = crearMesaPublicada(admin, materia, 5);
        long inscripcionId = objectMapper.readTree(inscribir(alumno, mesa).body()).get("id").asLong();

        MvcResult propio = mockMvc.perform(get("/api/v1/certificados/inscripcion-final/" + inscripcionId)
                        .header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(propio.getResponse().getContentType()).startsWith("application/pdf");
        assertThat(propio.getResponse().getHeader("Content-Disposition")).contains("comprobante-final-" + inscripcionId);
        byte[] pdf = propio.getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        assertThat(new String(pdf, pdf.length - 32, 32)).contains("%%EOF");

        // Otro alumno: la inscripcion "no existe" para el (no se filtra su existencia)
        mockMvc.perform(get("/api/v1/certificados/inscripcion-final/" + inscripcionId)
                        .header("Authorization", "Bearer " + ajeno.token()))
                .andExpect(status().isNotFound());
        // Un alumno tampoco puede usar la ruta del personal
        mockMvc.perform(get("/api/v1/certificados/alumno/" + alumno.id() + "/inscripcion-final/" + inscripcionId)
                        .header("Authorization", "Bearer " + ajeno.token()))
                .andExpect(status().isForbidden());
        // El personal si
        mockMvc.perform(get("/api/v1/certificados/alumno/" + alumno.id() + "/inscripcion-final/" + inscripcionId)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
    }

    @Test
    void inscripcionesAjenas_noSePuedenVerNiCancelar_yElListadoDeInscriptosEsDelPersonal() throws Exception {
        String admin = admin();
        Alumno duenio = crearAlumno(admin);
        Alumno ajeno = crearAlumno(admin);
        long materia = crearMateria(admin);
        fijarEstado(admin, duenio.id(), materia, EstadoMateria.REGULAR);
        long mesa = crearMesaPublicada(admin, materia, 5);
        long inscripcionId = objectMapper.readTree(inscribir(duenio, mesa).body()).get("id").asLong();

        // /me del ajeno no incluye la inscripcion del duenio
        String propias = mockMvc.perform(get("/api/v1/inscripciones-final/me").header("Authorization", "Bearer " + ajeno.token()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(propias).size()).isZero();
        // No puede cancelarla ni modificarla
        mockMvc.perform(delete("/api/v1/inscripciones-final/" + inscripcionId).header("Authorization", "Bearer " + ajeno.token()))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/inscripciones-final/" + inscripcionId)
                        .header("Authorization", "Bearer " + ajeno.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mesaId\":" + mesa + "}"))
                .andExpect(status().isNotFound());
        // Ni consultar inscripciones de otro alumno, ni el listado de inscriptos de una mesa
        mockMvc.perform(get("/api/v1/inscripciones-final/alumno/" + duenio.id()).header("Authorization", "Bearer " + ajeno.token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/inscripciones-final/mesa/" + mesa).header("Authorization", "Bearer " + ajeno.token()))
                .andExpect(status().isForbidden());
        // El personal ve a los inscriptos con sus datos
        MvcResult inscriptos = mockMvc.perform(get("/api/v1/inscripciones-final/mesa/" + mesa).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].alumnoId").value(duenio.id()))
                .andReturn();
        JsonNode fila = objectMapper.readTree(inscriptos.getResponse().getContentAsString()).get(0);
        assertThat(fila.get("alumnoDni").asText()).isNotBlank();
    }

    @Test
    void bajaDeInscripcion_conservaElHistorial_yNoSePuedeBajarDosVeces() throws Exception {
        String admin = admin();
        Alumno alumno = crearAlumno(admin);
        long materia = crearMateria(admin);
        fijarEstado(admin, alumno.id(), materia, EstadoMateria.REGULAR);
        long mesa = crearMesaPublicada(admin, materia, 5);
        long inscripcionId = objectMapper.readTree(inscribir(alumno, mesa).body()).get("id").asLong();

        mockMvc.perform(delete("/api/v1/inscripciones-final/" + inscripcionId).header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/inscripciones-final/" + inscripcionId).header("Authorization", "Bearer " + alumno.token()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("INSCRIPCION_DADA_DE_BAJA"));
        mockMvc.perform(get("/api/v1/inscripciones-final/me").header("Authorization", "Bearer " + alumno.token()))
                .andExpect(jsonPath("$[0].estado").value("BAJA"));
        // Liberado el lugar y el intento, puede volver a inscribirse
        assertThat(inscribir(alumno, mesa).status()).isEqualTo(200);
    }

    @Test
    void validacionesDeFechasYVentana_enLaCreacionDeMesas() throws Exception {
        String admin = admin();
        long materia = crearMateria(admin);

        // fecha pasada
        MesaExamenRequest pasada = new MesaExamenRequest(materia, LocalDate.now().minusDays(1), Turno.MANANA, 5, null, null,
                null, null, null, null, null);
        mockMvc.perform(post("/api/v1/mesas-examen").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(pasada)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("MESA_FUERA_DE_FECHA"));
        // cierre anterior a la apertura
        MesaExamenRequest incoherente = requestMesa(materia, 5, LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(2));
        mockMvc.perform(post("/api/v1/mesas-examen").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(incoherente)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("MESA_VENTANA_INVALIDA"));
        // materia inexistente
        MesaExamenRequest sinMateria = requestMesa(999999L, 5, null, null);
        mockMvc.perform(post("/api/v1/mesas-examen").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(sinMateria)))
                .andExpect(status().isNotFound());
        // cupo invalido (bean validation)
        mockMvc.perform(post("/api/v1/mesas-examen").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"materiaId\":" + materia + ",\"fecha\":\"" + LocalDate.now().plusDays(20) + "\",\"turno\":\"MANANA\",\"cupoMaximo\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modificarMesa_conInscriptos_noPermiteBajarElCupoPorDebajoNiCambiarLaMateria() throws Exception {
        String admin = admin();
        Alumno alumno = crearAlumno(admin);
        long materia = crearMateria(admin);
        long otraMateria = crearMateria(admin);
        fijarEstado(admin, alumno.id(), materia, EstadoMateria.REGULAR);
        MesaExamenRequest request = requestMesa(materia, 3, null, null);
        long mesa = crearMesaBorrador(admin, request);
        cambiarEstadoMesa(admin, mesa, "publicar");
        inscribir(alumno, mesa);

        MesaExamenRequest conMenosCupo = new MesaExamenRequest(materia, request.fecha(), request.turno(), 1, null, null, null,
                null, null, null, null);
        // cupo 1 con 1 inscripto: valido
        mockMvc.perform(put("/api/v1/mesas-examen/" + mesa).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(conMenosCupo)))
                .andExpect(status().isOk());
        MesaExamenRequest cambioMateria = new MesaExamenRequest(otraMateria, request.fecha(), request.turno(), 3, null, null,
                null, null, null, null, null);
        mockMvc.perform(put("/api/v1/mesas-examen/" + mesa).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(cambioMateria)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("MESA_CON_INSCRIPCIONES"));
    }
}
