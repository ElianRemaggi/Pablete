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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidadorAntelacionMinimaTest {

    @Mock
    private ConfiguracionInstitucionalRepository configuracionRepository;

    private ValidadorAntelacionMinima validador;
    private Alumno alumno;

    @BeforeEach
    void setUp() {
        validador = new ValidadorAntelacionMinima(configuracionRepository);
        alumno = Alumno.builder().usuarioId(1L).build();
        when(configuracionRepository.findById(1)).thenReturn(Optional.of(
                ConfiguracionInstitucional.builder().horasBloqueoFinal(48).build()));
    }

    @Test
    void validar_conMesaMuyCercana_lanzaBusinessException() {
        // Mesa "hoy": atStartOfDay() ya paso, siempre da menos de 48hs de antelacion.
        MesaExamen mesa = MesaExamen.builder().id(1L).fecha(LocalDate.now()).build();

        assertThatThrownBy(() -> validador.validar(alumno, mesa, null))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("FUERA_DE_PLAZO");
    }

    @Test
    void validar_conMesaLejana_noLanzaNada() {
        MesaExamen mesa = MesaExamen.builder().id(1L).fecha(LocalDate.now().plusDays(10)).build();

        assertThatCode(() -> validador.validar(alumno, mesa, null)).doesNotThrowAnyException();
    }
}
