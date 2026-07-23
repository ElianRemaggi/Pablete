package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.EstadoMateria;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.service.interfaces.EstadoAcademicoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidadorRegularidadTest {

    @Mock
    private EstadoAcademicoService estadoAcademicoService;

    private ValidadorRegularidad validador;
    private Alumno alumno;
    private MesaExamen mesa;

    @BeforeEach
    void setUp() {
        validador = new ValidadorRegularidad(estadoAcademicoService);
        alumno = Alumno.builder().usuarioId(1L).build();
        mesa = MesaExamen.builder().id(1L).materia(Materia.builder().id(1L).build()).build();
    }

    @Test
    void validar_conMateriaPendiente_lanzaBusinessException() {
        when(estadoAcademicoService.obtenerEstado(1L, 1L)).thenReturn(EstadoMateria.PENDIENTE);

        assertThatThrownBy(() -> validador.validar(alumno, mesa, null))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("MATERIA_NO_REGULARIZADA");
    }

    @Test
    void validar_conMateriaRegular_noLanzaNada() {
        when(estadoAcademicoService.obtenerEstado(1L, 1L)).thenReturn(EstadoMateria.REGULAR);

        assertThatCode(() -> validador.validar(alumno, mesa, null)).doesNotThrowAnyException();
    }

    @Test
    void validar_conMateriaAprobada_noLanzaNada() {
        when(estadoAcademicoService.obtenerEstado(1L, 1L)).thenReturn(EstadoMateria.APROBADA);

        assertThatCode(() -> validador.validar(alumno, mesa, null)).doesNotThrowAnyException();
    }
}
