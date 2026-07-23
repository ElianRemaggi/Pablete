package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.EstadoMateria;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.service.interfaces.EstadoAcademicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Regla 1 de docs/07-reglas-negocio-validaciones.md SS2: REGULAR o APROBADA (rendir para mejorar nota). */
@Component
@Order(1)
@RequiredArgsConstructor
public class ValidadorRegularidad implements ValidadorInscripcionFinal {

    private final EstadoAcademicoService estadoAcademicoService;

    @Override
    public void validar(Alumno alumno, MesaExamen mesa, InscripcionFinal inscripcionExistente) {
        EstadoMateria estado = estadoAcademicoService.obtenerEstado(alumno.getUsuarioId(), mesa.getMateria().getId());
        if (estado != EstadoMateria.REGULAR && estado != EstadoMateria.APROBADA) {
            throw new BusinessException("MATERIA_NO_REGULARIZADA",
                    "Debe tener la materia regularizada (o aprobada) para inscribirse a este final");
        }
    }
}
