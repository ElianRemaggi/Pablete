package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.exception.BusinessException;

/** Mismo patron que ValidadorInscripcionCursada: inscripcionExistente es null en un alta. */
public interface ValidadorInscripcionFinal {

    void validar(Alumno alumno, MesaExamen mesa, InscripcionFinal inscripcionExistente) throws BusinessException;
}
