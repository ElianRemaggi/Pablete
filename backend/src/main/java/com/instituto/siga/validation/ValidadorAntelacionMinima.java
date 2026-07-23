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

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Regla 4 de docs/07-reglas-negocio-validaciones.md SS2. Tambien rige modificar/dar de baja
 * (unica regla que aplica ahi, ver docs SS2 "Modificación/baja").
 */
@Component
@Order(4)
@RequiredArgsConstructor
public class ValidadorAntelacionMinima implements ValidadorInscripcionFinal {

    private final ConfiguracionInstitucionalRepository configuracionRepository;

    @Override
    public void validar(Alumno alumno, MesaExamen mesa, InscripcionFinal inscripcionExistente) {
        ConfiguracionInstitucional config = configuracionRepository.findById(1)
                .orElseThrow(() -> new ResourceNotFoundException("Configuración institucional no inicializada"));

        long horasHastaLaMesa = Duration.between(LocalDateTime.now(), mesa.getFecha().atStartOfDay()).toHours();
        if (horasHastaLaMesa < config.getHorasBloqueoFinal()) {
            throw new BusinessException("FUERA_DE_PLAZO",
                    "Debe inscribirse, modificar o dar de baja con al menos "
                            + config.getHorasBloqueoFinal() + " horas de antelación a la mesa");
        }
    }
}
