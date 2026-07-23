package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.Comision;
import com.instituto.siga.entity.EstadoInscripcion;
import com.instituto.siga.entity.InscripcionCursada;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.repository.InscripcionCursadaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** Regla 3 de docs/07-reglas-negocio-validaciones.md SS1. */
@Component
@Order(3)
@RequiredArgsConstructor
public class ValidadorCupoDisponible implements ValidadorInscripcionCursada {

    private static final List<EstadoInscripcion> ACTIVOS = List.of(EstadoInscripcion.INSCRIPTO, EstadoInscripcion.MODIFICADA);

    private final InscripcionCursadaRepository inscripcionCursadaRepository;

    @Override
    public void validar(Alumno alumno, Comision comision, InscripcionCursada inscripcionExistente) {
        long ocupados = inscripcionCursadaRepository.countByComisionIdAndEstadoIn(comision.getId(), ACTIVOS);

        if (inscripcionExistente != null && inscripcionExistente.getComision().getId().equals(comision.getId())) {
            ocupados--; // la propia inscripcion ya esta contada en el cupo de esta misma comision
        }

        if (ocupados >= comision.getCupoMaximo()) {
            throw new BusinessException("CUPO_AGOTADO", "No hay cupo disponible en la comisión");
        }
    }
}
