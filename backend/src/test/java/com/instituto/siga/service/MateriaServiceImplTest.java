package com.instituto.siga.service;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.MateriaRequest;
import com.instituto.siga.dto.request.PlanMateriaRequest;
import com.instituto.siga.dto.response.MateriaResponse;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.PlanEstudio;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.MateriaMapper;
import com.instituto.siga.repository.MateriaRepository;
import com.instituto.siga.repository.PlanEstudioRepository;
import com.instituto.siga.repository.PlanMateriaRepository;
import com.instituto.siga.service.impl.MateriaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MateriaServiceImplTest {

    @Mock
    private MateriaRepository materiaRepository;
    @Mock
    private PlanEstudioRepository planEstudioRepository;
    @Mock
    private PlanMateriaRepository planMateriaRepository;
    @Mock
    private MateriaMapper materiaMapper;
    @Mock
    private AuditoriaPublisher auditoriaPublisher;

    private MateriaServiceImpl materiaService;

    @BeforeEach
    void setUp() {
        materiaService = new MateriaServiceImpl(materiaRepository, planEstudioRepository, planMateriaRepository,
                materiaMapper, auditoriaPublisher);
    }

    @Test
    void crear_conCodigoDuplicado_lanzaConflictException() {
        MateriaRequest request = new MateriaRequest("MAT101", "Programación I", 120);
        when(materiaRepository.existsByCodigo("MAT101")).thenReturn(true);

        assertThatThrownBy(() -> materiaService.crear(request))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("MATERIA_DUPLICADA");
    }

    @Test
    void crear_conDatosValidos_persisteActiva() {
        MateriaRequest request = new MateriaRequest("MAT101", "Programación I", 120);
        Materia mapeada = Materia.builder().codigo(request.codigo()).nombre(request.nombre())
                .cargaHoraria(request.cargaHoraria()).build();

        when(materiaRepository.existsByCodigo(any())).thenReturn(false);
        when(materiaMapper.toEntity(request)).thenReturn(mapeada);
        when(materiaRepository.save(any(Materia.class))).thenAnswer(inv -> inv.getArgument(0));
        when(materiaMapper.toResponse(any(Materia.class)))
                .thenReturn(new MateriaResponse(1L, "MAT101", "Programación I", 120, true));

        materiaService.crear(request);

        ArgumentCaptor<Materia> captor = ArgumentCaptor.forClass(Materia.class);
        verify(materiaRepository).save(captor.capture());
        assertThat(captor.getValue().isActiva()).isTrue();
    }

    @Test
    void obtenerPorId_conIdInexistente_lanzaResourceNotFoundException() {
        when(materiaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> materiaService.obtenerPorId(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void darDeBaja_marcaActivaComoFalse() {
        Materia materia = Materia.builder().id(1L).codigo("MAT101").nombre("N").cargaHoraria(100).activa(true).build();
        when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));
        when(materiaRepository.save(any(Materia.class))).thenAnswer(inv -> inv.getArgument(0));

        materiaService.darDeBaja(1L);

        assertThat(materia.isActiva()).isFalse();
    }

    @Test
    void asociarAPlan_conPlanInexistente_lanzaResourceNotFoundException() {
        Materia materia = Materia.builder().id(1L).codigo("MAT101").nombre("N").cargaHoraria(100).activa(true).build();
        when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));
        when(planEstudioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> materiaService.asociarAPlan(1L, 99L, new PlanMateriaRequest(1, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void asociarAPlan_yaAsociada_lanzaConflictException() {
        Materia materia = Materia.builder().id(1L).codigo("MAT101").nombre("N").cargaHoraria(100).activa(true).build();
        PlanEstudio plan = PlanEstudio.builder().id(2L).version("2024").vigente(true).build();
        when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));
        when(planEstudioRepository.findById(2L)).thenReturn(Optional.of(plan));
        when(planMateriaRepository.existsByPlanIdAndMateriaId(2L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> materiaService.asociarAPlan(1L, 2L, new PlanMateriaRequest(1, 1)))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("MATERIA_YA_ASOCIADA_AL_PLAN");
    }

    @Test
    void asociarAPlan_conDatosValidos_persiste() {
        Materia materia = Materia.builder().id(1L).codigo("MAT101").nombre("N").cargaHoraria(100).activa(true).build();
        PlanEstudio plan = PlanEstudio.builder().id(2L).version("2024").vigente(true).build();
        when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));
        when(planEstudioRepository.findById(2L)).thenReturn(Optional.of(plan));
        when(planMateriaRepository.existsByPlanIdAndMateriaId(2L, 1L)).thenReturn(false);

        materiaService.asociarAPlan(1L, 2L, new PlanMateriaRequest(1, 2));

        verify(planMateriaRepository).save(any());
    }
}
