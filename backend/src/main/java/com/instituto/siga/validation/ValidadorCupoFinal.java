package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.EstadoInscripcion;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.repository.InscripcionFinalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** Cupo de la mesa. El servicio toma un lock pesimista sobre la mesa antes de validar, de modo que
 * dos inscripciones simultaneas no puedan contar el mismo ultimo lugar. */
@Component
@Order(6)
@RequiredArgsConstructor
public class ValidadorCupoFinal implements ValidadorInscripcionFinal {

    private static final List<EstadoInscripcion> ACTIVOS = List.of(EstadoInscripcion.INSCRIPTO, EstadoInscripcion.MODIFICADA);

    private final InscripcionFinalRepository inscripcionFinalRepository;

    @Override
    public void validar(Alumno alumno, MesaExamen mesa, InscripcionFinal inscripcionExistente) {
        long ocupados = inscripcionFinalRepository.countByMesaIdAndEstadoIn(mesa.getId(), ACTIVOS);

        if (inscripcionExistente != null && inscripcionExistente.getMesa().getId().equals(mesa.getId())) {
            ocupados--; // la propia inscripcion ya esta contada en el cupo de esta misma mesa
        }

        if (ocupados >= mesa.getCupoMaximo()) {
            throw new BusinessException("CUPO_AGOTADO", "No hay cupo disponible en la mesa");
        }
    }
}
