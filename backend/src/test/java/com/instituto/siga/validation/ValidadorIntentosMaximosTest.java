package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.ConfiguracionInstitucional;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.repository.ConfiguracionInstitucionalRepository;
import com.instituto.siga.repository.IntentoFinalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidadorIntentosMaximosTest {

    @Mock
    private IntentoFinalRepository intentoFinalRepository;
    @Mock
    private ConfiguracionInstitucionalRepository configuracionRepository;

    private ValidadorIntentosMaximos validador;
    private Alumno alumno;
    private MesaExamen mesa;

    @BeforeEach
    void setUp() {
        validador = new ValidadorIntentosMaximos(intentoFinalRepository, configuracionRepository);
        alumno = Alumno.builder().usuarioId(1L).build();
        mesa = MesaExamen.builder().id(1L).materia(Materia.builder().id(1L).build()).build();
        when(configuracionRepository.findById(1)).thenReturn(Optional.of(
                ConfiguracionInstitucional.builder().limiteIntentosFinal(3).build()));
    }

    @Test
    void validar_conLimiteAlcanzado_lanzaBusinessException() {
        when(intentoFinalRepository.contarActivosPorAlumnoYMateria(any(), any(), any())).thenReturn(3L);

        assertThatThrownBy(() -> validador.validar(alumno, mesa, null))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("LIMITE_INTENTOS_SUPERADO");
    }

    @Test
    void validar_conIntentosDisponibles_noLanzaNada() {
        when(intentoFinalRepository.contarActivosPorAlumnoYMateria(any(), any(), any())).thenReturn(1L);

        assertThatCode(() -> validador.validar(alumno, mesa, null)).doesNotThrowAnyException();
    }

    @Test
    void validar_conLimiteAlcanzadoPeroModificandoLaMismaMateria_noLanzaNada() {
        when(intentoFinalRepository.contarActivosPorAlumnoYMateria(any(), any(), any())).thenReturn(3L);
        InscripcionFinal existente = InscripcionFinal.builder().id(5L)
                .mesa(MesaExamen.builder().id(2L).materia(mesa.getMateria()).build()).build();

        assertThatCode(() -> validador.validar(alumno, mesa, existente)).doesNotThrowAnyException();
    }
}
