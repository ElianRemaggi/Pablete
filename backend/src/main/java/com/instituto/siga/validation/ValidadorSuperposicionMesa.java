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

/** Regla 2 de docs/07-reglas-negocio-validaciones.md SS2: misma fecha y turno que otra mesa activa. */
@Component
@Order(2)
@RequiredArgsConstructor
public class ValidadorSuperposicionMesa implements ValidadorInscripcionFinal {

    private static final List<EstadoInscripcion> ACTIVOS = List.of(EstadoInscripcion.INSCRIPTO, EstadoInscripcion.MODIFICADA);

    private final InscripcionFinalRepository inscripcionFinalRepository;

    @Override
    public void validar(Alumno alumno, MesaExamen mesa, InscripcionFinal inscripcionExistente) {
        List<InscripcionFinal> activas = inscripcionFinalRepository.findByAlumnoIdAndEstadoIn(alumno.getUsuarioId(), ACTIVOS);

        for (InscripcionFinal otra : activas) {
            if (inscripcionExistente != null && otra.getId().equals(inscripcionExistente.getId())) {
                continue;
            }
            if (otra.getMesa().getId().equals(mesa.getId())) {
                continue;
            }
            if (otra.getMesa().getFecha().equals(mesa.getFecha()) && otra.getMesa().getTurno() == mesa.getTurno()) {
                throw new BusinessException("SUPERPOSICION_MESA",
                        "Ya tiene una inscripción a otra mesa el mismo día y turno");
            }
        }
    }
}
