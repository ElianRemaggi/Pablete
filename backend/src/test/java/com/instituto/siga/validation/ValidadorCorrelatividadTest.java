package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.Comision;
import com.instituto.siga.entity.Correlatividad;
import com.instituto.siga.entity.EstadoMateria;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.TipoRequisito;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.repository.CorrelatividadRepository;
import com.instituto.siga.repository.MateriaRepository;
import com.instituto.siga.service.impl.RequisitosAcademicosServiceImpl;
import com.instituto.siga.service.interfaces.EstadoAcademicoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidadorCorrelatividadTest {

    @Mock
    private CorrelatividadRepository correlatividadRepository;
    @Mock
    private EstadoAcademicoService estadoAcademicoService;

    private ValidadorCorrelatividad validador;

    private Alumno alumno;
    private Materia materiaRequerida;
    private Comision comision;

    @BeforeEach
    void setUp() {
        validador = new ValidadorCorrelatividad(new RequisitosAcademicosServiceImpl(
                correlatividadRepository, estadoAcademicoService, org.mockito.Mockito.mock(MateriaRepository.class)));
        alumno = Alumno.builder().usuarioId(1L).legajo("LEG-0001").build();
        materiaRequerida = Materia.builder().id(1L).codigo("MAT101").nombre("Programación I").build();
        Materia materiaDestino = Materia.builder().id(2L).codigo("MAT102").nombre("Programación II").build();
        comision = Comision.builder().id(1L).materia(materiaDestino).build();
    }

    @Test
    void validar_sinCorrelatividades_noLanzaNada() {
        when(correlatividadRepository.findByMateriaId(2L)).thenReturn(List.of());

        assertThatCode(() -> validador.validar(alumno, comision, null)).doesNotThrowAnyException();
    }

    @Test
    void validar_requisitoAprobarSinAprobar_lanzaBusinessException() {
        Correlatividad correlatividad = Correlatividad.builder()
                .materiaRequerida(materiaRequerida).tipoRequisito(TipoRequisito.APROBAR).build();
        when(correlatividadRepository.findByMateriaId(2L)).thenReturn(List.of(correlatividad));
        when(estadoAcademicoService.obtenerEstado(1L, 1L)).thenReturn(EstadoMateria.REGULAR);

        assertThatThrownBy(() -> validador.validar(alumno, comision, null))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("REGLA_NEGOCIO_CORRELATIVIDAD");
    }

    @Test
    void validar_requisitoAprobarConAprobada_noLanzaNada() {
        Correlatividad correlatividad = Correlatividad.builder()
                .materiaRequerida(materiaRequerida).tipoRequisito(TipoRequisito.APROBAR).build();
        when(correlatividadRepository.findByMateriaId(2L)).thenReturn(List.of(correlatividad));
        when(estadoAcademicoService.obtenerEstado(1L, 1L)).thenReturn(EstadoMateria.APROBADA);

        assertThatCode(() -> validador.validar(alumno, comision, null)).doesNotThrowAnyException();
    }

    @Test
    void validar_requisitoCursarConRegular_noLanzaNada() {
        Correlatividad correlatividad = Correlatividad.builder()
                .materiaRequerida(materiaRequerida).tipoRequisito(TipoRequisito.CURSAR).build();
        when(correlatividadRepository.findByMateriaId(2L)).thenReturn(List.of(correlatividad));
        when(estadoAcademicoService.obtenerEstado(1L, 1L)).thenReturn(EstadoMateria.REGULAR);

        assertThatCode(() -> validador.validar(alumno, comision, null)).doesNotThrowAnyException();
    }

    @Test
    void validar_requisitoCursarConPendiente_lanzaBusinessException() {
        Correlatividad correlatividad = Correlatividad.builder()
                .materiaRequerida(materiaRequerida).tipoRequisito(TipoRequisito.CURSAR).build();
        when(correlatividadRepository.findByMateriaId(2L)).thenReturn(List.of(correlatividad));
        when(estadoAcademicoService.obtenerEstado(1L, 1L)).thenReturn(EstadoMateria.PENDIENTE);

        assertThatThrownBy(() -> validador.validar(alumno, comision, null))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("REGLA_NEGOCIO_CORRELATIVIDAD");
    }
}
