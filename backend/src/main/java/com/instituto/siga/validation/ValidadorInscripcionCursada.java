package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.Comision;
import com.instituto.siga.entity.InscripcionCursada;
import com.instituto.siga.exception.BusinessException;

/**
 * Firma extendida respecto al ejemplo de docs/07-reglas-negocio-validaciones.md SS5 (que solo
 * toma alumno+comision): se agrega inscripcionExistente (null en un alta) para que
 * ValidadorSuperposicionHoraria y ValidadorCupoDisponible puedan excluir la inscripcion que se
 * esta reemplazando al modificar, en vez de contarla como un conflicto contra si misma.
 */
public interface ValidadorInscripcionCursada {

    void validar(Alumno alumno, Comision comision, InscripcionCursada inscripcionExistente) throws BusinessException;
}
