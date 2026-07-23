package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.Comision;
import com.instituto.siga.entity.ConfiguracionInstitucional;
import com.instituto.siga.entity.InscripcionCursada;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.repository.ConfiguracionInstitucionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Regla 4 de docs/07-reglas-negocio-validaciones.md SS1. */
@Component
@Order(4)
@RequiredArgsConstructor
public class ValidadorVentanaInscripcion implements ValidadorInscripcionCursada {

    private final ConfiguracionInstitucionalRepository configuracionRepository;

    @Override
    public void validar(Alumno alumno, Comision comision, InscripcionCursada inscripcionExistente) {
        ConfiguracionInstitucional config = configuracionRepository.findById(1)
                .orElseThrow(() -> new ResourceNotFoundException("Configuración institucional no inicializada"));

        LocalDateTime ahora = LocalDateTime.now();
        if (ahora.isBefore(config.getFechaAperturaCursadas()) || ahora.isAfter(config.getFechaCierreCursadas())) {
            throw new BusinessException("FUERA_DE_PLAZO", "Fuera del período de inscripción a cursadas");
        }
    }
}
