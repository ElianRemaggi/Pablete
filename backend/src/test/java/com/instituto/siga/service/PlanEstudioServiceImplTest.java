package com.instituto.siga.service;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.PlanEstudioRequest;
import com.instituto.siga.dto.response.PlanEstudioResponse;
import com.instituto.siga.entity.Carrera;
import com.instituto.siga.entity.PlanEstudio;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.PlanEstudioMapper;
import com.instituto.siga.repository.CarreraRepository;
import com.instituto.siga.repository.PlanEstudioRepository;
import com.instituto.siga.service.impl.PlanEstudioServiceImpl;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanEstudioServiceImplTest {

    @Mock
    private PlanEstudioRepository planEstudioRepository;
    @Mock
    private CarreraRepository carreraRepository;
    @Mock
    private PlanEstudioMapper planEstudioMapper;
    @Mock
    private AuditoriaPublisher auditoriaPublisher;

    private PlanEstudioServiceImpl planEstudioService;

    @BeforeEach
    void setUp() {
        planEstudioService = new PlanEstudioServiceImpl(planEstudioRepository, carreraRepository,
                planEstudioMapper, auditoriaPublisher);
    }

    @Test
    void crear_conCarreraInexistente_lanzaResourceNotFoundException() {
        when(carreraRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planEstudioService.crear(1L, new PlanEstudioRequest("2024", true)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_conVersionDuplicadaParaLaCarrera_lanzaConflictException() {
        Carrera carrera = Carrera.builder().id(1L).codigo("TSW").nombre("N").activa(true).build();
        when(carreraRepository.findById(1L)).thenReturn(Optional.of(carrera));
        when(planEstudioRepository.existsByCarreraIdAndVersion(1L, "2024")).thenReturn(true);

        assertThatThrownBy(() -> planEstudioService.crear(1L, new PlanEstudioRequest("2024", true)))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("PLAN_ESTUDIO_DUPLICADO");
    }

    @Test
    void crear_conDatosValidos_persisteVigentePorDefecto() {
        Carrera carrera = Carrera.builder().id(1L).codigo("TSW").nombre("N").activa(true).build();
        when(carreraRepository.findById(1L)).thenReturn(Optional.of(carrera));
        when(planEstudioRepository.existsByCarreraIdAndVersion(1L, "2024")).thenReturn(false);
        when(planEstudioRepository.save(any(PlanEstudio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(planEstudioMapper.toResponse(any(PlanEstudio.class)))
                .thenReturn(new PlanEstudioResponse(1L, 1L, "2024", true));

        planEstudioService.crear(1L, new PlanEstudioRequest("2024", null));

        ArgumentCaptor<PlanEstudio> captor = ArgumentCaptor.forClass(PlanEstudio.class);
        org.mockito.Mockito.verify(planEstudioRepository).save(captor.capture());
        assertThat(captor.getValue().isVigente()).isTrue();
    }

    @Test
    void actualizar_conVersionDuplicadaDeOtroPlan_lanzaConflictException() {
        Carrera carrera = Carrera.builder().id(1L).codigo("TSW").nombre("N").activa(true).build();
        PlanEstudio plan = PlanEstudio.builder().id(5L).carrera(carrera).version("2023").vigente(true).build();
        when(planEstudioRepository.findById(5L)).thenReturn(Optional.of(plan));
        when(planEstudioRepository.existsByCarreraIdAndVersionAndIdNot(1L, "2024", 5L)).thenReturn(true);

        assertThatThrownBy(() -> planEstudioService.actualizar(5L, new PlanEstudioRequest("2024", true)))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("PLAN_ESTUDIO_DUPLICADO");
    }

    @Test
    void actualizar_conIdInexistente_lanzaResourceNotFoundException() {
        when(planEstudioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planEstudioService.actualizar(999L, new PlanEstudioRequest("2024", true)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
