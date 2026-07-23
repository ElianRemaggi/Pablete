package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.Comision;
import com.instituto.siga.entity.EstadoInscripcion;
import com.instituto.siga.entity.Horario;
import com.instituto.siga.entity.InscripcionCursada;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.repository.InscripcionCursadaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** Regla 2 de docs/07-reglas-negocio-validaciones.md SS1: mismo dia, rango horario intersectante, mismo periodo lectivo. */
@Component
@Order(2)
@RequiredArgsConstructor
public class ValidadorSuperposicionHoraria implements ValidadorInscripcionCursada {

    private static final List<EstadoInscripcion> ACTIVOS = List.of(EstadoInscripcion.INSCRIPTO, EstadoInscripcion.MODIFICADA);

    private final InscripcionCursadaRepository inscripcionCursadaRepository;

    @Override
    public void validar(Alumno alumno, Comision comision, InscripcionCursada inscripcionExistente) {
        List<InscripcionCursada> activas = inscripcionCursadaRepository.findByAlumnoIdAndEstadoIn(alumno.getUsuarioId(), ACTIVOS);

        for (InscripcionCursada otra : activas) {
            if (inscripcionExistente != null && otra.getId().equals(inscripcionExistente.getId())) {
                continue; // es la inscripcion que se esta reemplazando, no cuenta contra si misma
            }
            if (otra.getComision().getId().equals(comision.getId())) {
                continue; // misma comision
            }
            if (!otra.getComision().getPeriodoLectivo().equals(comision.getPeriodoLectivo())) {
                continue; // la superposicion solo importa dentro del mismo periodo lectivo
            }

            for (Horario nuevo : comision.getHorarios()) {
                for (Horario existente : otra.getComision().getHorarios()) {
                    if (seSuperponen(nuevo, existente)) {
                        throw new BusinessException("SUPERPOSICION_HORARIA",
                                "El horario se superpone con la comisión " + otra.getComision().getNombre()
                                        + " de " + otra.getComision().getMateria().getNombre());
                    }
                }
            }
        }
    }

    private boolean seSuperponen(Horario a, Horario b) {
        return a.getDiaSemana() == b.getDiaSemana()
                && a.getHoraInicio().isBefore(b.getHoraFin())
                && b.getHoraInicio().isBefore(a.getHoraFin());
    }
}
