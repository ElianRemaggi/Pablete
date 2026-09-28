package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.response.EstadoAcademicoResponse;
import com.instituto.siga.entity.EstadoMateria;

public interface EstadoAcademicoService {

    /** PENDIENTE si el alumno nunca curso/rindio la materia. */
    EstadoMateria obtenerEstado(Long alumnoId, Long materiaId);

    /** Efecto de inscribirse a una comision; no pisa un estado ya mas avanzado que PENDIENTE. */
    void marcarCursando(Long alumnoId, Long materiaId);

    /** Override administrativo (docs no define el flujo real todavia: aprobar un final).
     * nota es opcional (1-10); null si el estado no tiene nota asociada (ej. CURSANDO). */
    EstadoAcademicoResponse fijarEstado(Long alumnoId, Long materiaId, EstadoMateria estado, Integer nota);
}
