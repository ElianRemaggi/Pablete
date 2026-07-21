package com.instituto.siga.service;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.CorrelatividadRequest;
import com.instituto.siga.dto.response.CorrelatividadResponse;
import com.instituto.siga.entity.Correlatividad;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.TipoRequisito;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.CorrelatividadMapper;
import com.instituto.siga.repository.CorrelatividadRepository;
import com.instituto.siga.repository.MateriaRepository;
import com.instituto.siga.service.impl.CorrelatividadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorrelatividadServiceImplTest {

    @Mock
    private CorrelatividadRepository correlatividadRepository;
    @Mock
    private MateriaRepository materiaRepository;
    @Mock
    private CorrelatividadMapper correlatividadMapper;
    @Mock
    private AuditoriaPublisher auditoriaPublisher;

    private CorrelatividadServiceImpl correlatividadService;

    @BeforeEach
    void setUp() {
        correlatividadService = new CorrelatividadServiceImpl(correlatividadRepository, materiaRepository, correlatividadMapper, auditoriaPublisher);
        lenient().when(materiaRepository.findById(anyLong()))
                .thenAnswer(inv -> Optional.of(Materia.builder().id(inv.getArgument(0)).codigo("M" + inv.getArgument(0)).nombre("Materia " + inv.getArgument(0)).build()));
    }

    @Test
    void crear_conAutoreferencia_lanzaBusinessException() {
        CorrelatividadRequest request = new CorrelatividadRequest(1L, 1L, TipoRequisito.APROBAR);

        assertThatThrownBy(() -> correlatividadService.crear(request))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("CORRELATIVIDAD_AUTOREFERENCIA");
    }

    @Test
    void crear_conMateriaInexistente_lanzaResourceNotFoundException() {
        when(materiaRepository.findById(1L)).thenReturn(Optional.empty());
        CorrelatividadRequest request = new CorrelatividadRequest(1L, 2L, TipoRequisito.APROBAR);

        assertThatThrownBy(() -> correlatividadService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_conCorrelatividadYaExistente_lanzaConflictException() {
        CorrelatividadRequest request = new CorrelatividadRequest(1L, 2L, TipoRequisito.APROBAR);
        when(correlatividadRepository.existsByMateriaIdAndMateriaRequeridaIdAndTipoRequisito(1L, 2L, TipoRequisito.APROBAR))
                .thenReturn(true);

        assertThatThrownBy(() -> correlatividadService.crear(request))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("CORRELATIVIDAD_DUPLICADA");
    }

    @Test
    void crear_conCicloDirecto_lanzaBusinessException() {
        // Ya existe: materia 2 requiere materia 1 (arista 2 -> 1).
        when(correlatividadRepository.buscarAristas()).thenReturn(List.<Object[]>of(new Object[]{2L, 1L}));
        // Se intenta: materia 1 requiere materia 2 -> cerraria el ciclo 1 -> 2 -> 1.
        CorrelatividadRequest request = new CorrelatividadRequest(1L, 2L, TipoRequisito.CURSAR);

        assertThatThrownBy(() -> correlatividadService.crear(request))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("CORRELATIVIDAD_CICLICA");
    }

    @Test
    void crear_conCicloIndirecto_lanzaBusinessException() {
        // Ya existe: materia 1 requiere materia 2, materia 2 requiere materia 3.
        when(correlatividadRepository.buscarAristas())
                .thenReturn(List.<Object[]>of(new Object[]{1L, 2L}, new Object[]{2L, 3L}));
        // Se intenta: materia 3 requiere materia 1 -> cerraria el ciclo 1 -> 2 -> 3 -> 1.
        CorrelatividadRequest request = new CorrelatividadRequest(3L, 1L, TipoRequisito.CURSAR);

        assertThatThrownBy(() -> correlatividadService.crear(request))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("CORRELATIVIDAD_CICLICA");
    }

    @Test
    void crear_sinCiclo_persisteCorrectamente() {
        when(correlatividadRepository.buscarAristas()).thenReturn(List.of());
        when(correlatividadRepository.save(any(Correlatividad.class))).thenAnswer(inv -> inv.getArgument(0));
        when(correlatividadMapper.toResponse(any(Correlatividad.class))).thenReturn(
                new CorrelatividadResponse(1L, 1L, "Materia 1", 2L, "Materia 2", "APROBAR"));

        CorrelatividadRequest request = new CorrelatividadRequest(1L, 2L, TipoRequisito.APROBAR);
        CorrelatividadResponse response = correlatividadService.crear(request);

        org.assertj.core.api.Assertions.assertThat(response.tipoRequisito()).isEqualTo("APROBAR");
    }
}
