package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.response.HabilitacionFinalResponse;
import com.instituto.siga.dto.response.RequisitoResponse;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.Materia;

import java.util.List;

/** Unico lugar donde se evaluan las correlatividades de cursada y de final (docs/07): lo usan los validadores
 * de ambos pipelines y las consultas de "que me falta", para que todos los endpoints apliquen las mismas reglas. */
public interface RequisitosAcademicosService {

    /** Correlatividades configuradas de la materia, cada una evaluada contra el estado academico del alumno.
     * Para cursar: CURSAR -> REGULAR/APROBADA, APROBAR -> APROBADA. Para rendir el final (paraFinal): toda correlativa,
     * sea cual sea su tipo, exige el FINAL aprobado de la materia requerida (APROBADA). */
    List<RequisitoResponse> evaluarCorrelativas(Long alumnoId, Materia materia, boolean paraFinal);

    /** Para inscribirse a la cursada: todas las correlativas cumplidas (CURSAR -> REGULAR/APROBADA, APROBAR -> APROBADA). */
    void validarParaCursada(Alumno alumno, Materia materia);

    /** Para inscribirse al final: cursada propia aprobada (REGULAR/APROBADA) y las mismas correlativas. */
    void validarParaFinal(Alumno alumno, Materia materia);

    HabilitacionFinalResponse habilitacionFinalPropia(Long alumnoId, Long materiaId);

    HabilitacionFinalResponse habilitacionFinalDeAlumno(Long alumnoId, Long materiaId);
}
