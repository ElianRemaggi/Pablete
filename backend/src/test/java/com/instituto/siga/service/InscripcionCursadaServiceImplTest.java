package com.instituto.siga.service;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.InscripcionCursadaRequest;
import com.instituto.siga.dto.response.InscripcionCursadaResponse;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.Comision;
import com.instituto.siga.entity.ConfiguracionInstitucional;
import com.instituto.siga.entity.EstadoInscripcion;
import com.instituto.siga.entity.InscripcionCursada;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.InscripcionCursadaMapper;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.ComisionRepository;
import com.instituto.siga.repository.ConfiguracionInstitucionalRepository;
import com.instituto.siga.repository.InscripcionCursadaRepository;
import com.instituto.siga.service.impl.InscripcionCursadaServiceImpl;
import com.instituto.siga.service.interfaces.EstadoAcademicoService;
import com.instituto.siga.validation.ValidadorInscripcionCursada;
import com.instituto.siga.validation.ValidadorVentanaInscripcion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InscripcionCursadaServiceImplTest {

    @Mock
    private InscripcionCursadaRepository inscripcionCursadaRepository;
    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private ComisionRepository comisionRepository;
    @Mock
    private ConfiguracionInstitucionalRepository configuracionRepository;
    @Mock
    private EstadoAcademicoService estadoAcademicoService;
    @Mock
    private InscripcionCursadaMapper inscripcionCursadaMapper;
    @Mock
    private AuditoriaPublisher auditoriaPublisher;
    @Mock
    private ValidadorVentanaInscripcion validadorVentana;
    @Mock
    private ValidadorInscripcionCursada otroValidador;

    private InscripcionCursadaServiceImpl service;

    private Alumno alumno;
    private Comision comision;

    @BeforeEach
    void setUp() {
        service = new InscripcionCursadaServiceImpl(inscripcionCursadaRepository, alumnoRepository, comisionRepository,
                configuracionRepository, estadoAcademicoService, inscripcionCursadaMapper, auditoriaPublisher,
                List.of(otroValidador, validadorVentana));

        alumno = Alumno.builder().usuarioId(1L).build();
        comision = Comision.builder().id(1L).materia(Materia.builder().id(1L).nombre("Materia").build()).build();

        lenient().when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
        lenient().when(comisionRepository.findById(1L)).thenReturn(Optional.of(comision));
    }

    @Test
    void inscribir_conInscripcionYaActiva_lanzaConflictException() {
        when(inscripcionCursadaRepository.existsByAlumnoIdAndComisionIdAndEstadoIn(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.inscribir(1L, new InscripcionCursadaRequest(1L)))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("INSCRIPCION_DUPLICADA");

        verifyNoInteractions(otroValidador, validadorVentana);
    }

    @Test
    void inscribir_conValidadorQueRechaza_propagaLaExcepcionYNoPersiste() {
        when(inscripcionCursadaRepository.existsByAlumnoIdAndComisionIdAndEstadoIn(any(), any(), any())).thenReturn(false);
        doThrow(new BusinessException("CUPO_AGOTADO", "sin cupo")).when(otroValidador).validar(alumno, comision, null);

        assertThatThrownBy(() -> service.inscribir(1L, new InscripcionCursadaRequest(1L)))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("CUPO_AGOTADO");

        verify(inscripcionCursadaRepository, never()).save(any());
    }

    @Test
    void inscribir_valido_persisteMarcaCursandoYAudita() {
        when(inscripcionCursadaRepository.existsByAlumnoIdAndComisionIdAndEstadoIn(any(), any(), any())).thenReturn(false);
        when(inscripcionCursadaRepository.save(any(InscripcionCursada.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inscripcionCursadaMapper.toResponse(any(InscripcionCursada.class))).thenReturn(
                new InscripcionCursadaResponse(1L, "Materia", "A", "INSCRIPTO", LocalDateTime.now()));

        InscripcionCursadaResponse response = service.inscribir(1L, new InscripcionCursadaRequest(1L));

        assertThat(response.estado()).isEqualTo("INSCRIPTO");
        verify(otroValidador).validar(alumno, comision, null);
        verify(validadorVentana).validar(alumno, comision, null);
        verify(estadoAcademicoService).marcarCursando(1L, 1L);
        verify(auditoriaPublisher).publicar(eq(1L), eq("INSCRIPCION_CURSADA_ALTA"), eq("InscripcionCursada"), any(), any());
    }

    @Test
    void modificar_deInscripcionQueNoLePertenece_lanzaResourceNotFoundException() {
        when(inscripcionCursadaRepository.findByIdAndAlumnoId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modificar(1L, 99L, new InscripcionCursadaRequest(1L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void modificar_conVentanaDeCierrePasada_lanzaBusinessException() {
        InscripcionCursada propia = InscripcionCursada.builder().id(5L).alumno(alumno).comision(comision)
                .estado(EstadoInscripcion.INSCRIPTO).build();
        when(inscripcionCursadaRepository.findByIdAndAlumnoId(5L, 1L)).thenReturn(Optional.of(propia));
        when(configuracionRepository.findById(1)).thenReturn(Optional.of(
                ConfiguracionInstitucional.builder().fechaCierreCursadas(LocalDateTime.now().minusDays(1)).build()));

        assertThatThrownBy(() -> service.modificar(1L, 5L, new InscripcionCursadaRequest(1L)))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("FUERA_DE_PLAZO");
    }

    @Test
    void modificar_saltaElValidadorDeVentanaPeroCorreLosDemas() {
        InscripcionCursada propia = InscripcionCursada.builder().id(5L).alumno(alumno).comision(comision)
                .estado(EstadoInscripcion.INSCRIPTO).build();
        when(inscripcionCursadaRepository.findByIdAndAlumnoId(5L, 1L)).thenReturn(Optional.of(propia));
        when(configuracionRepository.findById(1)).thenReturn(Optional.of(
                ConfiguracionInstitucional.builder().fechaCierreCursadas(LocalDateTime.now().plusDays(10)).build()));
        when(inscripcionCursadaRepository.save(any(InscripcionCursada.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inscripcionCursadaMapper.toResponse(any(InscripcionCursada.class))).thenReturn(
                new InscripcionCursadaResponse(5L, "Materia", "A", "MODIFICADA", LocalDateTime.now()));

        service.modificar(1L, 5L, new InscripcionCursadaRequest(1L));

        verify(otroValidador).validar(alumno, comision, propia);
        verify(validadorVentana, never()).validar(any(), any(), any());
    }

    @Test
    void listarPorAlumno_delegaEnElRepositorioYMapeaCadaResultado() {
        InscripcionCursada propia = InscripcionCursada.builder().id(5L).alumno(alumno).comision(comision)
                .estado(EstadoInscripcion.BAJA).build();
        when(inscripcionCursadaRepository.findByAlumnoId(1L)).thenReturn(List.of(propia));
        when(inscripcionCursadaMapper.toResponse(propia)).thenReturn(
                new InscripcionCursadaResponse(5L, "Materia", "A", "BAJA", LocalDateTime.now()));

        List<InscripcionCursadaResponse> resultado = service.listarPorAlumno(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).estado()).isEqualTo("BAJA");
    }

    @Test
    void darDeBaja_marcaEstadoBajaYAudita() {
        InscripcionCursada propia = InscripcionCursada.builder().id(5L).alumno(alumno).comision(comision)
                .estado(EstadoInscripcion.INSCRIPTO).build();
        when(inscripcionCursadaRepository.findByIdAndAlumnoId(5L, 1L)).thenReturn(Optional.of(propia));
        when(configuracionRepository.findById(1)).thenReturn(Optional.of(
                ConfiguracionInstitucional.builder().fechaCierreCursadas(LocalDateTime.now().plusDays(10)).build()));
        when(inscripcionCursadaRepository.save(any(InscripcionCursada.class))).thenAnswer(inv -> inv.getArgument(0));

        service.darDeBaja(1L, 5L);

        assertThat(propia.getEstado()).isEqualTo(EstadoInscripcion.BAJA);
        verify(auditoriaPublisher).publicar(eq(1L), eq("INSCRIPCION_CURSADA_BAJA"), eq("InscripcionCursada"), any(), any());
    }
}
