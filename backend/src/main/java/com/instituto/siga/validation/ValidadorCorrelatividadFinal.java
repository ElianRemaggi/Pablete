package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.service.interfaces.RequisitosAcademicosService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Correlatividades configuradas de la materia de la mesa: CURSAR exige cursada aprobada de la requerida y
 * APROBAR exige su final aprobado. Corre despues de ValidadorRegularidad (cursada propia aprobada). */
@Component
@Order(2)
@RequiredArgsConstructor
public class ValidadorCorrelatividadFinal implements ValidadorInscripcionFinal {

    private final RequisitosAcademicosService requisitosService;

    @Override
    public void validar(Alumno alumno, MesaExamen mesa, InscripcionFinal inscripcionExistente) {
        requisitosService.validarParaFinal(alumno, mesa.getMateria());
    }
}
