package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.EstadoInscripcion;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.entity.Turno;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.repository.InscripcionFinalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidadorSuperposicionMesaTest {

    @Mock
    private InscripcionFinalRepository inscripcionFinalRepository;

    private ValidadorSuperposicionMesa validador;
    private Alumno alumno;

    @BeforeEach
    void setUp() {
        validador = new ValidadorSuperposicionMesa(inscripcionFinalRepository);
        alumno = Alumno.builder().usuarioId(1L).build();
    }

    private MesaExamen mesaCon(Long id, LocalDate fecha, Turno turno) {
        return MesaExamen.builder().id(id).materia(Materia.builder().id(1L).nombre("Materia").build())
                .fecha(fecha).turno(turno).build();
    }

    @Test
    void validar_conOtraMesaMismaFechaYTurno_lanzaBusinessException() {
        MesaExamen existente = mesaCon(1L, LocalDate.of(2026, 12, 1), Turno.MANANA);
        InscripcionFinal activa = InscripcionFinal.builder().id(10L).mesa(existente).estado(EstadoInscripcion.INSCRIPTO).build();
        when(inscripcionFinalRepository.findByAlumnoIdAndEstadoIn(any(), any())).thenReturn(List.of(activa));

        MesaExamen nueva = mesaCon(2L, LocalDate.of(2026, 12, 1), Turno.MANANA);

        assertThatThrownBy(() -> validador.validar(alumno, nueva, null))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("SUPERPOSICION_MESA");
    }

    @Test
    void validar_conOtraMesaMismaFechaDistintoTurno_noLanzaNada() {
        MesaExamen existente = mesaCon(1L, LocalDate.of(2026, 12, 1), Turno.MANANA);
        InscripcionFinal activa = InscripcionFinal.builder().id(10L).mesa(existente).estado(EstadoInscripcion.INSCRIPTO).build();
        when(inscripcionFinalRepository.findByAlumnoIdAndEstadoIn(any(), any())).thenReturn(List.of(activa));

        MesaExamen nueva = mesaCon(2L, LocalDate.of(2026, 12, 1), Turno.TARDE);

        assertThatCode(() -> validador.validar(alumno, nueva, null)).doesNotThrowAnyException();
    }

    @Test
    void validar_excluyendoLaPropiaInscripcionAlModificar_noLanzaNada() {
        MesaExamen existente = mesaCon(1L, LocalDate.of(2026, 12, 1), Turno.MANANA);
        InscripcionFinal propia = InscripcionFinal.builder().id(10L).mesa(existente).estado(EstadoInscripcion.INSCRIPTO).build();
        when(inscripcionFinalRepository.findByAlumnoIdAndEstadoIn(any(), any())).thenReturn(List.of(propia));

        MesaExamen nueva = mesaCon(2L, LocalDate.of(2026, 12, 1), Turno.MANANA);

        assertThatCode(() -> validador.validar(alumno, nueva, propia)).doesNotThrowAnyException();
    }
}
