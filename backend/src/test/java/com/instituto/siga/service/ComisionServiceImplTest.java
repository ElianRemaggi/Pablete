package com.instituto.siga.service;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.ComisionRequest;
import com.instituto.siga.dto.request.HorarioRequest;
import com.instituto.siga.dto.response.ComisionResponse;
import com.instituto.siga.entity.Comision;
import com.instituto.siga.entity.Horario;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.ComisionMapper;
import com.instituto.siga.mapper.HorarioMapper;
import com.instituto.siga.repository.ComisionRepository;
import com.instituto.siga.repository.MateriaRepository;
import com.instituto.siga.service.impl.ComisionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComisionServiceImplTest {

    @Mock
    private ComisionRepository comisionRepository;
    @Mock
    private MateriaRepository materiaRepository;
    @Mock
    private ComisionMapper comisionMapper;
    @Mock
    private HorarioMapper horarioMapper;
    @Mock
    private AuditoriaPublisher auditoriaPublisher;

    private ComisionServiceImpl comisionService;

    @BeforeEach
    void setUp() {
        comisionService = new ComisionServiceImpl(comisionRepository, materiaRepository, comisionMapper, horarioMapper, auditoriaPublisher);
        lenient().when(materiaRepository.findById(1L))
                .thenReturn(Optional.of(Materia.builder().id(1L).codigo("MAT101").nombre("Programación I").build()));
        lenient().when(horarioMapper.toEntity(any(HorarioRequest.class))).thenAnswer(inv -> {
            HorarioRequest r = inv.getArgument(0);
            return Horario.builder().diaSemana(r.diaSemana()).horaInicio(r.horaInicio()).horaFin(r.horaFin()).aula(r.aula()).build();
        });
        lenient().when(comisionRepository.save(any(Comision.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(comisionMapper.toResponse(any(Comision.class)))
                .thenReturn(new ComisionResponse(1L, 1L, "Programación I", "A", "2026-1C", 40, List.of()));
    }

    @Test
    void crear_conComisionDuplicada_lanzaConflictException() {
        when(comisionRepository.existsByMateriaIdAndNombreAndPeriodoLectivo(1L, "A", "2026-1C")).thenReturn(true);
        ComisionRequest request = new ComisionRequest(1L, "A", "2026-1C", 40, List.of());

        assertThatThrownBy(() -> comisionService.crear(request))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("COMISION_DUPLICADA");
    }

    @Test
    void crear_conHorarioInvalido_lanzaBusinessException() {
        HorarioRequest horarioInvalido = new HorarioRequest(1, LocalTime.of(20, 0), LocalTime.of(18, 0), "Aula 1");
        ComisionRequest request = new ComisionRequest(1L, "A", "2026-1C", 40, List.of(horarioInvalido));

        assertThatThrownBy(() -> comisionService.crear(request))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("HORARIO_INVALIDO");
    }

    @Test
    void crear_conHorariosSuperpuestosEntreSi_lanzaBusinessException() {
        HorarioRequest h1 = new HorarioRequest(1, LocalTime.of(18, 0), LocalTime.of(22, 0), "Aula 1");
        HorarioRequest h2 = new HorarioRequest(1, LocalTime.of(19, 0), LocalTime.of(21, 0), "Aula 2");
        ComisionRequest request = new ComisionRequest(1L, "A", "2026-1C", 40, List.of(h1, h2));

        assertThatThrownBy(() -> comisionService.crear(request))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("HORARIO_SUPERPUESTO");
    }

    @Test
    void crear_conHorariosValidosSinSuperposicion_persisteComision() {
        HorarioRequest h1 = new HorarioRequest(1, LocalTime.of(18, 0), LocalTime.of(22, 0), "Aula 1");
        HorarioRequest h2 = new HorarioRequest(3, LocalTime.of(18, 0), LocalTime.of(22, 0), "Aula 1");
        ComisionRequest request = new ComisionRequest(1L, "A", "2026-1C", 40, List.of(h1, h2));

        comisionService.crear(request);
        // No exception -> valido.
    }

    @Test
    void agregarHorario_queSeSuperponeConExistente_lanzaBusinessException() {
        Horario existente = Horario.builder().id(1L).diaSemana(1).horaInicio(LocalTime.of(18, 0)).horaFin(LocalTime.of(22, 0)).build();
        Comision comision = Comision.builder().id(1L).materia(Materia.builder().id(1L).build())
                .horarios(new ArrayList<>(List.of(existente))).build();
        when(comisionRepository.findById(1L)).thenReturn(Optional.of(comision));

        HorarioRequest nuevo = new HorarioRequest(1, LocalTime.of(19, 0), LocalTime.of(20, 0), "Aula 2");

        assertThatThrownBy(() -> comisionService.agregarHorario(1L, nuevo))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("HORARIO_SUPERPUESTO");
    }

    @Test
    void quitarHorario_queNoExiste_lanzaResourceNotFoundException() {
        Comision comision = Comision.builder().id(1L).horarios(new ArrayList<>()).build();
        when(comisionRepository.findById(1L)).thenReturn(Optional.of(comision));

        assertThatThrownBy(() -> comisionService.quitarHorario(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listar_sinFiltros_delegaEnFindAll() {
        when(comisionRepository.findAll()).thenReturn(List.of());
        List<ComisionResponse> resultado = comisionService.listar(null, null);
        assertThat(resultado).isEmpty();
    }
}
