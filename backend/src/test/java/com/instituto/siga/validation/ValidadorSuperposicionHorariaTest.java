package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.Comision;
import com.instituto.siga.entity.EstadoInscripcion;
import com.instituto.siga.entity.Horario;
import com.instituto.siga.entity.InscripcionCursada;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.repository.InscripcionCursadaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidadorSuperposicionHorariaTest {

    @Mock
    private InscripcionCursadaRepository inscripcionCursadaRepository;

    private ValidadorSuperposicionHoraria validador;
    private Alumno alumno;

    @BeforeEach
    void setUp() {
        validador = new ValidadorSuperposicionHoraria(inscripcionCursadaRepository);
        alumno = Alumno.builder().usuarioId(1L).build();
    }

    private Comision comisionCon(Long id, String periodo, int dia, LocalTime inicio, LocalTime fin) {
        Comision comision = Comision.builder().id(id).nombre("A")
                .materia(Materia.builder().id(1L).nombre("Materia").build())
                .periodoLectivo(periodo).build();
        Horario horario = Horario.builder().comision(comision).diaSemana(dia).horaInicio(inicio).horaFin(fin).build();
        comision.setHorarios(List.of(horario));
        return comision;
    }

    @Test
    void validar_conHorarioSuperpuestoEnMismoPeriodo_lanzaBusinessException() {
        Comision existente = comisionCon(1L, "2026-1C", 1, LocalTime.of(18, 0), LocalTime.of(22, 0));
        InscripcionCursada activa = InscripcionCursada.builder().id(10L).comision(existente)
                .estado(EstadoInscripcion.INSCRIPTO).build();
        when(inscripcionCursadaRepository.findByAlumnoIdAndEstadoIn(any(), any())).thenReturn(List.of(activa));

        Comision nueva = comisionCon(2L, "2026-1C", 1, LocalTime.of(19, 0), LocalTime.of(21, 0));

        assertThatThrownBy(() -> validador.validar(alumno, nueva, null))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("SUPERPOSICION_HORARIA");
    }

    @Test
    void validar_conHorarioEnDiaDistinto_noLanzaNada() {
        Comision existente = comisionCon(1L, "2026-1C", 1, LocalTime.of(18, 0), LocalTime.of(22, 0));
        InscripcionCursada activa = InscripcionCursada.builder().id(10L).comision(existente)
                .estado(EstadoInscripcion.INSCRIPTO).build();
        when(inscripcionCursadaRepository.findByAlumnoIdAndEstadoIn(any(), any())).thenReturn(List.of(activa));

        Comision nueva = comisionCon(2L, "2026-1C", 3, LocalTime.of(18, 0), LocalTime.of(22, 0));

        assertThatCode(() -> validador.validar(alumno, nueva, null)).doesNotThrowAnyException();
    }

    @Test
    void validar_conHorarioSuperpuestoEnOtroPeriodoLectivo_noLanzaNada() {
        Comision existente = comisionCon(1L, "2025-2C", 1, LocalTime.of(18, 0), LocalTime.of(22, 0));
        InscripcionCursada activa = InscripcionCursada.builder().id(10L).comision(existente)
                .estado(EstadoInscripcion.INSCRIPTO).build();
        when(inscripcionCursadaRepository.findByAlumnoIdAndEstadoIn(any(), any())).thenReturn(List.of(activa));

        Comision nueva = comisionCon(2L, "2026-1C", 1, LocalTime.of(18, 0), LocalTime.of(22, 0));

        assertThatCode(() -> validador.validar(alumno, nueva, null)).doesNotThrowAnyException();
    }

    @Test
    void validar_excluyendoLaPropiaInscripcionAlModificar_noLanzaNada() {
        Comision existente = comisionCon(1L, "2026-1C", 1, LocalTime.of(18, 0), LocalTime.of(22, 0));
        InscripcionCursada propia = InscripcionCursada.builder().id(10L).comision(existente)
                .estado(EstadoInscripcion.INSCRIPTO).build();
        when(inscripcionCursadaRepository.findByAlumnoIdAndEstadoIn(any(), any())).thenReturn(List.of(propia));

        // Se esta "modificando" hacia una comision con el mismo horario que la propia inscripcion actual.
        Comision nueva = comisionCon(2L, "2026-1C", 1, LocalTime.of(18, 0), LocalTime.of(22, 0));

        assertThatCode(() -> validador.validar(alumno, nueva, propia)).doesNotThrowAnyException();
    }
}
