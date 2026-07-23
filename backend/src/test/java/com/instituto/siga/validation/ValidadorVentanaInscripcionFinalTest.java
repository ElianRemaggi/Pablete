package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.ConfiguracionInstitucional;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.repository.ConfiguracionInstitucionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidadorVentanaInscripcionFinalTest {

    @Mock
    private ConfiguracionInstitucionalRepository configuracionRepository;

    private ValidadorVentanaInscripcionFinal validador;
    private Alumno alumno;
    private MesaExamen mesa;

    @BeforeEach
    void setUp() {
        validador = new ValidadorVentanaInscripcionFinal(configuracionRepository);
        alumno = Alumno.builder().usuarioId(1L).build();
        mesa = MesaExamen.builder().id(1L).build();
    }

    @Test
    void validar_conVentanaCerrada_lanzaBusinessException() {
        when(configuracionRepository.findById(1)).thenReturn(Optional.of(ConfiguracionInstitucional.builder()
                .fechaAperturaFinales(LocalDateTime.now().minusDays(60))
                .fechaCierreFinales(LocalDateTime.now().minusDays(1))
                .build()));

        assertThatThrownBy(() -> validador.validar(alumno, mesa, null))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("FUERA_DE_PLAZO");
    }

    @Test
    void validar_conVentanaAbierta_noLanzaNada() {
        when(configuracionRepository.findById(1)).thenReturn(Optional.of(ConfiguracionInstitucional.builder()
                .fechaAperturaFinales(LocalDateTime.now().minusDays(1))
                .fechaCierreFinales(LocalDateTime.now().plusDays(1))
                .build()));

        assertThatCode(() -> validador.validar(alumno, mesa, null)).doesNotThrowAnyException();
    }
}
