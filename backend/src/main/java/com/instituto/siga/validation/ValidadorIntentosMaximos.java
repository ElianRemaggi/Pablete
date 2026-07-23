package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.ConfiguracionInstitucional;
import com.instituto.siga.entity.EstadoInscripcion;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.repository.ConfiguracionInstitucionalRepository;
import com.instituto.siga.repository.IntentoFinalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** Regla 3 de docs/07-reglas-negocio-validaciones.md SS2. */
@Component
@Order(3)
@RequiredArgsConstructor
public class ValidadorIntentosMaximos implements ValidadorInscripcionFinal {

    private static final List<EstadoInscripcion> ACTIVOS = List.of(EstadoInscripcion.INSCRIPTO, EstadoInscripcion.MODIFICADA);

    private final IntentoFinalRepository intentoFinalRepository;
    private final ConfiguracionInstitucionalRepository configuracionRepository;

    @Override
    public void validar(Alumno alumno, MesaExamen mesa, InscripcionFinal inscripcionExistente) {
        ConfiguracionInstitucional config = configuracionRepository.findById(1)
                .orElseThrow(() -> new ResourceNotFoundException("Configuración institucional no inicializada"));

        long intentosUsados = intentoFinalRepository.contarActivosPorAlumnoYMateria(
                alumno.getUsuarioId(), mesa.getMateria().getId(), ACTIVOS);

        // Si se esta modificando una inscripcion de la MISMA materia, ese intento ya esta
        // contado y no debe sumar como uno adicional.
        if (inscripcionExistente != null && inscripcionExistente.getMesa().getMateria().getId().equals(mesa.getMateria().getId())) {
            intentosUsados--;
        }

        if (intentosUsados >= config.getLimiteIntentosFinal()) {
            throw new BusinessException("LIMITE_INTENTOS_SUPERADO",
                    "Superó el límite de intentos permitidos para rendir esta materia");
        }
    }
}
