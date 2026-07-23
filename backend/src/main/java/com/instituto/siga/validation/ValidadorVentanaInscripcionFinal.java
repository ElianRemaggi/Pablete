package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.ConfiguracionInstitucional;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.repository.ConfiguracionInstitucionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Regla 5 de docs/07-reglas-negocio-validaciones.md SS2. Solo se corre en el alta (ver SS2 "Modificación/baja"). */
@Component
@Order(5)
@RequiredArgsConstructor
public class ValidadorVentanaInscripcionFinal implements ValidadorInscripcionFinal {

    private final ConfiguracionInstitucionalRepository configuracionRepository;

    @Override
    public void validar(Alumno alumno, MesaExamen mesa, InscripcionFinal inscripcionExistente) {
        ConfiguracionInstitucional config = configuracionRepository.findById(1)
                .orElseThrow(() -> new ResourceNotFoundException("Configuración institucional no inicializada"));

        LocalDateTime ahora = LocalDateTime.now();
        if (ahora.isBefore(config.getFechaAperturaFinales()) || ahora.isAfter(config.getFechaCierreFinales())) {
            throw new BusinessException("FUERA_DE_PLAZO", "Fuera del período de inscripción a finales");
        }
    }
}
