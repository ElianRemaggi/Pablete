package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.Comision;
import com.instituto.siga.entity.InscripcionCursada;
import com.instituto.siga.service.interfaces.RequisitosAcademicosService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Regla 1 de docs/07-reglas-negocio-validaciones.md SS1; la evaluacion vive en RequisitosAcademicosService. */
@Component
@Order(1)
@RequiredArgsConstructor
public class ValidadorCorrelatividad implements ValidadorInscripcionCursada {

    private final RequisitosAcademicosService requisitosService;

    @Override
    public void validar(Alumno alumno, Comision comision, InscripcionCursada inscripcionExistente) {
        requisitosService.validarParaCursada(alumno, comision.getMateria());
    }
}
