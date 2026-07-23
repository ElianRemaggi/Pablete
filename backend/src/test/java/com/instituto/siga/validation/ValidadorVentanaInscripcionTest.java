package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.Comision;
import com.instituto.siga.entity.ConfiguracionInstitucional;
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
class ValidadorVentanaInscripcionTest {

    @Mock
    private ConfiguracionInstitucionalRepository configuracionRepository;

    private ValidadorVentanaInscripcion validador;
    private Alumno alumno;
    private Comision comision;

    @BeforeEach
    void setUp() {
        validador = new ValidadorVentanaInscripcion(configuracionRepository);
        alumno = Alumno.builder().usuarioId(1L).build();
        comision = Comision.builder().id(1L).build();
    }

    @Test
    void validar_conVentanaCerrada_lanzaBusinessException() {
        ConfiguracionInstitucional config = ConfiguracionInstitucional.builder()
                .fechaAperturaCursadas(LocalDateTime.now().minusDays(60))
                .fechaCierreCursadas(LocalDateTime.now().minusDays(1))
                .build();
        when(configuracionRepository.findById(1)).thenReturn(Optional.of(config));

        assertThatThrownBy(() -> validador.validar(alumno, comision, null))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("FUERA_DE_PLAZO");
    }

    @Test
    void validar_conVentanaAbierta_noLanzaNada() {
        ConfiguracionInstitucional config = ConfiguracionInstitucional.builder()
                .fechaAperturaCursadas(LocalDateTime.now().minusDays(1))
                .fechaCierreCursadas(LocalDateTime.now().plusDays(1))
                .build();
        when(configuracionRepository.findById(1)).thenReturn(Optional.of(config));

        assertThatCode(() -> validador.validar(alumno, comision, null)).doesNotThrowAnyException();
    }
}
