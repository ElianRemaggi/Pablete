package com.instituto.siga.service;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.ConfiguracionInstitucionalRequest;
import com.instituto.siga.dto.response.ConfiguracionInstitucionalResponse;
import com.instituto.siga.entity.ConfiguracionInstitucional;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.ConfiguracionInstitucionalMapper;
import com.instituto.siga.repository.ConfiguracionInstitucionalRepository;
import com.instituto.siga.service.impl.ConfiguracionInstitucionalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracionInstitucionalServiceImplTest {

    @Mock
    private ConfiguracionInstitucionalRepository configuracionRepository;
    @Mock
    private ConfiguracionInstitucionalMapper configuracionMapper;
    @Mock
    private AuditoriaPublisher auditoriaPublisher;

    private ConfiguracionInstitucionalServiceImpl configuracionService;

    @BeforeEach
    void setUp() {
        configuracionService = new ConfiguracionInstitucionalServiceImpl(configuracionRepository, configuracionMapper, auditoriaPublisher);
    }

    @Test
    void obtener_sinFilaInicializada_lanzaResourceNotFoundException() {
        when(configuracionRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configuracionService.obtener())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizar_conCierreCursadasAntesDeApertura_lanzaBusinessException() {
        LocalDateTime ahora = LocalDateTime.now();
        ConfiguracionInstitucionalRequest request = new ConfiguracionInstitucionalRequest(
                ahora, ahora.minusDays(1), ahora, ahora.plusDays(30), 3, 48);

        assertThatThrownBy(() -> configuracionService.actualizar(request))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("CONFIGURACION_FECHAS_INVALIDAS");
    }

    @Test
    void actualizar_conCierreFinalesAntesDeApertura_lanzaBusinessException() {
        LocalDateTime ahora = LocalDateTime.now();
        ConfiguracionInstitucionalRequest request = new ConfiguracionInstitucionalRequest(
                ahora, ahora.plusDays(30), ahora, ahora.minusDays(1), 3, 48);

        assertThatThrownBy(() -> configuracionService.actualizar(request))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("CONFIGURACION_FECHAS_INVALIDAS");
    }

    @Test
    void actualizar_conFechasValidas_persisteLosNuevosValores() {
        LocalDateTime ahora = LocalDateTime.now();
        ConfiguracionInstitucionalRequest request = new ConfiguracionInstitucionalRequest(
                ahora, ahora.plusDays(30), ahora, ahora.plusDays(60), 5, 72);

        when(configuracionRepository.findById(1))
                .thenReturn(Optional.of(ConfiguracionInstitucional.builder().id(1).build()));
        when(configuracionRepository.save(any(ConfiguracionInstitucional.class))).thenAnswer(inv -> inv.getArgument(0));
        when(configuracionMapper.toResponse(any(ConfiguracionInstitucional.class))).thenAnswer(inv -> {
            ConfiguracionInstitucional c = inv.getArgument(0);
            return new ConfiguracionInstitucionalResponse(c.getFechaAperturaCursadas(), c.getFechaCierreCursadas(),
                    c.getFechaAperturaFinales(), c.getFechaCierreFinales(), c.getLimiteIntentosFinal(), c.getHorasBloqueoFinal());
        });

        ConfiguracionInstitucionalResponse response = configuracionService.actualizar(request);

        assertThat(response.limiteIntentosFinal()).isEqualTo(5);
        assertThat(response.horasBloqueoFinal()).isEqualTo(72);
    }
}
