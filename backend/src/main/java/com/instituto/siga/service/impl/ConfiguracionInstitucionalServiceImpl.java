package com.instituto.siga.service.impl;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.ConfiguracionInstitucionalRequest;
import com.instituto.siga.dto.response.ConfiguracionInstitucionalResponse;
import com.instituto.siga.entity.ConfiguracionInstitucional;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.ConfiguracionInstitucionalMapper;
import com.instituto.siga.repository.ConfiguracionInstitucionalRepository;
import com.instituto.siga.security.SecurityUtils;
import com.instituto.siga.service.interfaces.ConfiguracionInstitucionalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Fila unica (docs/02 SS1, CHECK id = 1). */
@Service
@RequiredArgsConstructor
public class ConfiguracionInstitucionalServiceImpl implements ConfiguracionInstitucionalService {

    private static final Integer ID_UNICO = 1;

    private final ConfiguracionInstitucionalRepository configuracionRepository;
    private final ConfiguracionInstitucionalMapper configuracionMapper;
    private final AuditoriaPublisher auditoriaPublisher;

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionInstitucionalResponse obtener() {
        return configuracionMapper.toResponse(buscarOFallar());
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public ConfiguracionInstitucionalResponse actualizar(ConfiguracionInstitucionalRequest request) {
        validarFechas(request);

        ConfiguracionInstitucional configuracion = configuracionRepository.findById(ID_UNICO)
                .orElseGet(() -> ConfiguracionInstitucional.builder().id(ID_UNICO).build());

        Map<String, Object> detalle = construirDiff(configuracion, request);

        configuracion.setFechaAperturaCursadas(request.fechaAperturaCursadas());
        configuracion.setFechaCierreCursadas(request.fechaCierreCursadas());
        configuracion.setFechaAperturaFinales(request.fechaAperturaFinales());
        configuracion.setFechaCierreFinales(request.fechaCierreFinales());
        configuracion.setLimiteIntentosFinal(request.limiteIntentosFinal());
        configuracion.setHorasBloqueoFinal(request.horasBloqueoFinal());

        ConfiguracionInstitucional guardada = configuracionRepository.save(configuracion);

        if (!detalle.isEmpty()) {
            auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "CONFIGURACION_MODIFICADA",
                    "ConfiguracionInstitucional", ID_UNICO.longValue(), detalle);
        }

        return configuracionMapper.toResponse(guardada);
    }

    private ConfiguracionInstitucional buscarOFallar() {
        return configuracionRepository.findById(ID_UNICO)
                .orElseThrow(() -> new ResourceNotFoundException("Configuración institucional no inicializada"));
    }

    private void validarFechas(ConfiguracionInstitucionalRequest request) {
        if (!request.fechaCierreCursadas().isAfter(request.fechaAperturaCursadas())) {
            throw new BusinessException("CONFIGURACION_FECHAS_INVALIDAS",
                    "La fecha de cierre de cursadas debe ser posterior a la de apertura");
        }
        if (!request.fechaCierreFinales().isAfter(request.fechaAperturaFinales())) {
            throw new BusinessException("CONFIGURACION_FECHAS_INVALIDAS",
                    "La fecha de cierre de finales debe ser posterior a la de apertura");
        }
    }

    /** Detalle antes/despues por campo modificado, formato literal de docs/08-auditoria.md SS3. */
    private Map<String, Object> construirDiff(ConfiguracionInstitucional actual, ConfiguracionInstitucionalRequest nuevo) {
        Map<String, Object> diff = new LinkedHashMap<>();
        agregarSiCambio(diff, "fechaAperturaCursadas", actual.getFechaAperturaCursadas(), nuevo.fechaAperturaCursadas());
        agregarSiCambio(diff, "fechaCierreCursadas", actual.getFechaCierreCursadas(), nuevo.fechaCierreCursadas());
        agregarSiCambio(diff, "fechaAperturaFinales", actual.getFechaAperturaFinales(), nuevo.fechaAperturaFinales());
        agregarSiCambio(diff, "fechaCierreFinales", actual.getFechaCierreFinales(), nuevo.fechaCierreFinales());
        agregarSiCambio(diff, "limiteIntentosFinal", actual.getLimiteIntentosFinal(), nuevo.limiteIntentosFinal());
        agregarSiCambio(diff, "horasBloqueoFinal", actual.getHorasBloqueoFinal(), nuevo.horasBloqueoFinal());
        return diff;
    }

    private void agregarSiCambio(Map<String, Object> diff, String campo, Object antes, Object despues) {
        if (!Objects.equals(antes, despues)) {
            diff.put(campo, Map.of("antes", String.valueOf(antes), "despues", String.valueOf(despues)));
        }
    }
}
