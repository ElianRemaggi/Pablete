package com.instituto.siga.service;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.MesaExamenRequest;
import com.instituto.siga.dto.response.MesaExamenResponse;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.entity.Turno;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.mapper.MesaExamenMapper;
import com.instituto.siga.repository.MateriaRepository;
import com.instituto.siga.repository.MesaExamenRepository;
import com.instituto.siga.service.impl.MesaExamenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesaExamenServiceImplTest {

    @Mock
    private MesaExamenRepository mesaExamenRepository;
    @Mock
    private MateriaRepository materiaRepository;
    @Mock
    private MesaExamenMapper mesaExamenMapper;
    @Mock
    private AuditoriaPublisher auditoriaPublisher;

    private MesaExamenServiceImpl mesaExamenService;

    @BeforeEach
    void setUp() {
        mesaExamenService = new MesaExamenServiceImpl(mesaExamenRepository, materiaRepository, mesaExamenMapper, auditoriaPublisher);
        lenient().when(materiaRepository.findById(1L))
                .thenReturn(Optional.of(Materia.builder().id(1L).codigo("MAT101").nombre("Programación I").build()));
    }

    @Test
    void crear_conFechaPasada_lanzaBusinessException() {
        MesaExamenRequest request = new MesaExamenRequest(1L, LocalDate.now().minusDays(1), Turno.MANANA, 30);

        assertThatThrownBy(() -> mesaExamenService.crear(request))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("MESA_FUERA_DE_FECHA");
    }

    @Test
    void crear_conMesaYaExistente_lanzaConflictException() {
        LocalDate fecha = LocalDate.now().plusDays(30);
        when(mesaExamenRepository.existsByMateriaIdAndFechaAndTurno(1L, fecha, Turno.MANANA)).thenReturn(true);
        MesaExamenRequest request = new MesaExamenRequest(1L, fecha, Turno.MANANA, 30);

        assertThatThrownBy(() -> mesaExamenService.crear(request))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("MESA_DUPLICADA");
    }

    @Test
    void crear_conDatosValidos_persiste() {
        LocalDate fecha = LocalDate.now().plusDays(30);
        when(mesaExamenRepository.save(any(MesaExamen.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mesaExamenMapper.toResponse(any(MesaExamen.class)))
                .thenReturn(new MesaExamenResponse(1L, 1L, "Programación I", fecha, "MANANA", 30));

        MesaExamenResponse response = mesaExamenService.crear(new MesaExamenRequest(1L, fecha, Turno.MANANA, 30));

        org.assertj.core.api.Assertions.assertThat(response.turno()).isEqualTo("MANANA");
    }
}
