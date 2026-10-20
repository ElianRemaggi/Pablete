package com.instituto.siga.service;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.InscripcionFinalRequest;
import com.instituto.siga.dto.response.InscripcionFinalResponse;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.EstadoInscripcion;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.InscripcionFinalMapper;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.InscripcionFinalRepository;
import com.instituto.siga.repository.IntentoFinalRepository;
import com.instituto.siga.repository.MesaExamenRepository;
import com.instituto.siga.repository.PlanMateriaRepository;
import com.instituto.siga.service.impl.InscripcionFinalServiceImpl;
import com.instituto.siga.validation.ValidadorAntelacionMinima;
import com.instituto.siga.validation.ValidadorInscripcionFinal;
import com.instituto.siga.validation.ValidadorVentanaInscripcionFinal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InscripcionFinalServiceImplTest {

    @Mock
    private InscripcionFinalRepository inscripcionFinalRepository;
    @Mock
    private IntentoFinalRepository intentoFinalRepository;
    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private MesaExamenRepository mesaExamenRepository;
    @Mock
    private PlanMateriaRepository planMateriaRepository;
    @Mock
    private InscripcionFinalMapper inscripcionFinalMapper;
    @Mock
    private AuditoriaPublisher auditoriaPublisher;
    @Mock
    private ValidadorAntelacionMinima validadorAntelacionMinima;
    @Mock
    private ValidadorVentanaInscripcionFinal validadorVentana;
    @Mock
    private ValidadorInscripcionFinal otroValidador;

    private InscripcionFinalServiceImpl service;

    private Alumno alumno;
    private MesaExamen mesa;

    @BeforeEach
    void setUp() {
        service = new InscripcionFinalServiceImpl(inscripcionFinalRepository, intentoFinalRepository, alumnoRepository,
                mesaExamenRepository, planMateriaRepository, inscripcionFinalMapper, auditoriaPublisher,
                validadorAntelacionMinima, List.of(otroValidador, validadorAntelacionMinima, validadorVentana));

        alumno = Alumno.builder().usuarioId(1L).build();
        mesa = MesaExamen.builder().id(1L).materia(Materia.builder().id(1L).nombre("Materia").build())
                .fecha(LocalDate.now().plusDays(30)).build();

        lenient().when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
        lenient().when(mesaExamenRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(mesa));
        lenient().when(intentoFinalRepository.contarActivosPorAlumnoYMateria(any(), any(), any())).thenReturn(0L);
    }

    @Test
    void inscribir_conInscripcionYaActiva_lanzaConflictException() {
        when(inscripcionFinalRepository.existsByAlumnoIdAndMesaIdAndEstadoIn(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.inscribir(1L, new InscripcionFinalRequest(1L)))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("INSCRIPCION_DUPLICADA");
    }

    @Test
    void inscribir_conValidadorQueRechaza_propagaLaExcepcionYNoPersiste() {
        when(inscripcionFinalRepository.existsByAlumnoIdAndMesaIdAndEstadoIn(any(), any(), any())).thenReturn(false);
        doThrow(new BusinessException("MATERIA_NO_REGULARIZADA", "no regular")).when(otroValidador).validar(alumno, mesa, null);

        assertThatThrownBy(() -> service.inscribir(1L, new InscripcionFinalRequest(1L)))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("MATERIA_NO_REGULARIZADA");

        verify(inscripcionFinalRepository, never()).save(any());
    }

    @Test
    void inscribir_valido_persisteCreaIntentoYAudita() {
        when(inscripcionFinalRepository.existsByAlumnoIdAndMesaIdAndEstadoIn(any(), any(), any())).thenReturn(false);
        when(inscripcionFinalRepository.save(any(InscripcionFinal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inscripcionFinalMapper.toResponse(any(InscripcionFinal.class))).thenReturn(
                respuesta(1L, "INSCRIPTO"));

        InscripcionFinalResponse response = service.inscribir(1L, new InscripcionFinalRequest(1L));

        assertThat(response.estado()).isEqualTo("INSCRIPTO");
        verify(intentoFinalRepository).save(any());
        verify(auditoriaPublisher).publicar(eq(1L), eq("INSCRIPCION_FINAL_ALTA"), eq("InscripcionFinal"), any(), any());
    }

    @Test
    void modificar_deInscripcionQueNoLePertenece_lanzaResourceNotFoundException() {
        when(inscripcionFinalRepository.findByIdAndAlumnoId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modificar(1L, 99L, new InscripcionFinalRequest(1L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void modificar_conAntelacionInsuficienteEnMesaActual_propagaLaExcepcion() {
        InscripcionFinal propia = InscripcionFinal.builder().id(5L).alumno(alumno).mesa(mesa)
                .estado(EstadoInscripcion.INSCRIPTO).build();
        when(inscripcionFinalRepository.findByIdAndAlumnoId(5L, 1L)).thenReturn(Optional.of(propia));
        doThrow(new BusinessException("FUERA_DE_PLAZO", "tarde")).when(validadorAntelacionMinima).validar(alumno, mesa, propia);

        assertThatThrownBy(() -> service.modificar(1L, 5L, new InscripcionFinalRequest(1L)))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("FUERA_DE_PLAZO");
    }

    @Test
    void modificar_saltaElValidadorDeVentanaPeroCorreLosDemas() {
        InscripcionFinal propia = InscripcionFinal.builder().id(5L).alumno(alumno).mesa(mesa)
                .estado(EstadoInscripcion.INSCRIPTO).build();
        when(inscripcionFinalRepository.findByIdAndAlumnoId(5L, 1L)).thenReturn(Optional.of(propia));
        when(inscripcionFinalRepository.save(any(InscripcionFinal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inscripcionFinalMapper.toResponse(any(InscripcionFinal.class))).thenReturn(
                respuesta(5L, "MODIFICADA"));
        when(intentoFinalRepository.findByInscripcionFinalId(5L)).thenReturn(List.of());

        service.modificar(1L, 5L, new InscripcionFinalRequest(1L));

        verify(otroValidador).validar(alumno, mesa, propia);
        verify(validadorVentana, never()).validar(any(), any(), any());
    }

    @Test
    void listarPorAlumno_delegaEnElRepositorioYMapeaCadaResultado() {
        InscripcionFinal propia = InscripcionFinal.builder().id(5L).alumno(alumno).mesa(mesa)
                .estado(EstadoInscripcion.BAJA).build();
        when(inscripcionFinalRepository.findByAlumnoId(1L)).thenReturn(List.of(propia));
        when(inscripcionFinalMapper.toResponse(propia)).thenReturn(
                respuesta(5L, "BAJA"));

        List<InscripcionFinalResponse> resultado = service.listarPorAlumno(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).estado()).isEqualTo("BAJA");
    }

    @Test
    void darDeBaja_marcaEstadoBajaYBorraIntento() {
        InscripcionFinal propia = InscripcionFinal.builder().id(5L).alumno(alumno).mesa(mesa)
                .estado(EstadoInscripcion.INSCRIPTO).build();
        when(inscripcionFinalRepository.findByIdAndAlumnoId(5L, 1L)).thenReturn(Optional.of(propia));
        when(inscripcionFinalRepository.save(any(InscripcionFinal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(intentoFinalRepository.findByInscripcionFinalId(5L)).thenReturn(List.of());

        service.darDeBaja(1L, 5L);

        assertThat(propia.getEstado()).isEqualTo(EstadoInscripcion.BAJA);
        verify(auditoriaPublisher).publicar(eq(1L), eq("INSCRIPCION_FINAL_BAJA"), eq("InscripcionFinal"), any(), any());
    }

    private static InscripcionFinalResponse respuesta(long id, String estado) {
        return new InscripcionFinalResponse(id, 1L, "Materia", LocalDate.now().plusDays(30), "MANANA", estado, null,
                null, null, null, null, null, null, "PUBLICADA", LocalDateTime.now(), 1L, "30000000", "Nombre",
                "Apellido", "LEG-1");
    }
}
