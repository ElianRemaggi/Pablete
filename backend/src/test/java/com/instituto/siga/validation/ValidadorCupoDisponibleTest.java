package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.Comision;
import com.instituto.siga.entity.InscripcionCursada;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.repository.InscripcionCursadaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidadorCupoDisponibleTest {

    @Mock
    private InscripcionCursadaRepository inscripcionCursadaRepository;

    private ValidadorCupoDisponible validador;
    private Alumno alumno;

    @BeforeEach
    void setUp() {
        validador = new ValidadorCupoDisponible(inscripcionCursadaRepository);
        alumno = Alumno.builder().usuarioId(1L).build();
    }

    @Test
    void validar_conCupoLleno_lanzaBusinessException() {
        Comision comision = Comision.builder().id(1L).cupoMaximo(2).build();
        when(inscripcionCursadaRepository.countByComisionIdAndEstadoIn(any(), any())).thenReturn(2L);

        assertThatThrownBy(() -> validador.validar(alumno, comision, null))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("CUPO_AGOTADO");
    }

    @Test
    void validar_conCupoDisponible_noLanzaNada() {
        Comision comision = Comision.builder().id(1L).cupoMaximo(2).build();
        when(inscripcionCursadaRepository.countByComisionIdAndEstadoIn(any(), any())).thenReturn(1L);

        assertThatCode(() -> validador.validar(alumno, comision, null)).doesNotThrowAnyException();
    }

    @Test
    void validar_conCupoLlenoPeroExcluyendoLaPropiaEnEsaMismaComision_noLanzaNada() {
        Comision comision = Comision.builder().id(1L).cupoMaximo(1).build();
        InscripcionCursada propia = InscripcionCursada.builder().id(5L).comision(comision).build();
        when(inscripcionCursadaRepository.countByComisionIdAndEstadoIn(any(), any())).thenReturn(1L);

        assertThatCode(() -> validador.validar(alumno, comision, propia)).doesNotThrowAnyException();
    }
}
