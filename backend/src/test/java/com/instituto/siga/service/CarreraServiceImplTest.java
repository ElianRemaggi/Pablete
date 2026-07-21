package com.instituto.siga.service;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.CarreraRequest;
import com.instituto.siga.dto.response.CarreraResponse;
import com.instituto.siga.entity.Carrera;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.CarreraMapper;
import com.instituto.siga.repository.CarreraRepository;
import com.instituto.siga.service.impl.CarreraServiceImpl;
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
class CarreraServiceImplTest {

    @Mock
    private CarreraRepository carreraRepository;
    @Mock
    private CarreraMapper carreraMapper;
    @Mock
    private AuditoriaPublisher auditoriaPublisher;

    private CarreraServiceImpl carreraService;

    @BeforeEach
    void setUp() {
        carreraService = new CarreraServiceImpl(carreraRepository, carreraMapper, auditoriaPublisher);
    }

    @Test
    void crear_conCodigoDuplicado_lanzaConflictException() {
        CarreraRequest request = new CarreraRequest("TSW", "Tecnicatura en Sistemas Web");
        when(carreraRepository.existsByCodigo("TSW")).thenReturn(true);

        assertThatThrownBy(() -> carreraService.crear(request))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("CARRERA_DUPLICADA");
    }

    @Test
    void crear_conDatosValidos_persisteActiva() {
        CarreraRequest request = new CarreraRequest("TSW", "Tecnicatura en Sistemas Web");
        Carrera mapeada = Carrera.builder().codigo(request.codigo()).nombre(request.nombre()).build();

        when(carreraRepository.existsByCodigo(any())).thenReturn(false);
        when(carreraRepository.existsByNombre(any())).thenReturn(false);
        when(carreraMapper.toEntity(request)).thenReturn(mapeada);
        when(carreraRepository.save(any(Carrera.class))).thenAnswer(inv -> inv.getArgument(0));
        when(carreraMapper.toResponse(any(Carrera.class)))
                .thenReturn(new CarreraResponse(1L, "TSW", "Tecnicatura en Sistemas Web", true));

        carreraService.crear(request);

        ArgumentCaptor<Carrera> captor = ArgumentCaptor.forClass(Carrera.class);
        verify(carreraRepository).save(captor.capture());
        assertThat(captor.getValue().isActiva()).isTrue();
    }

    @Test
    void actualizar_conIdInexistente_lanzaResourceNotFoundException() {
        when(carreraRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> carreraService.actualizar(999L, new CarreraRequest("X", "Y")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void darDeBaja_marcaActivaComoFalse() {
        Carrera carrera = Carrera.builder().id(1L).codigo("TSW").nombre("N").activa(true).build();
        when(carreraRepository.findById(1L)).thenReturn(Optional.of(carrera));
        when(carreraRepository.save(any(Carrera.class))).thenAnswer(inv -> inv.getArgument(0));

        carreraService.darDeBaja(1L);

        assertThat(carrera.isActiva()).isFalse();
    }
}
