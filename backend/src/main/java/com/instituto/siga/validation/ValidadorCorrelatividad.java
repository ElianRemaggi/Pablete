package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.Comision;
import com.instituto.siga.entity.Correlatividad;
import com.instituto.siga.entity.EstadoMateria;
import com.instituto.siga.entity.InscripcionCursada;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.repository.CorrelatividadRepository;
import com.instituto.siga.service.interfaces.EstadoAcademicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Regla 1 de docs/07-reglas-negocio-validaciones.md SS1. */
@Component
@Order(1)
@RequiredArgsConstructor
public class ValidadorCorrelatividad implements ValidadorInscripcionCursada {

    private final CorrelatividadRepository correlatividadRepository;
    private final EstadoAcademicoService estadoAcademicoService;

    @Override
    public void validar(Alumno alumno, Comision comision, InscripcionCursada inscripcionExistente) {
        for (Correlatividad correlatividad : correlatividadRepository.findByMateriaId(comision.getMateria().getId())) {
            EstadoMateria estado = estadoAcademicoService.obtenerEstado(
                    alumno.getUsuarioId(), correlatividad.getMateriaRequerida().getId());

            boolean cumple = switch (correlatividad.getTipoRequisito()) {
                case CURSAR -> estado == EstadoMateria.REGULAR || estado == EstadoMateria.APROBADA;
                case APROBAR -> estado == EstadoMateria.APROBADA;
            };

            if (!cumple) {
                throw new BusinessException("REGLA_NEGOCIO_CORRELATIVIDAD",
                        "No cumple la correlatividad requerida para inscribirse a esta materia.",
                        java.util.List.of("Materia requerida: " + correlatividad.getMateriaRequerida().getNombre()
                                + " (" + correlatividad.getTipoRequisito() + ")"));
            }
        }
    }
}
