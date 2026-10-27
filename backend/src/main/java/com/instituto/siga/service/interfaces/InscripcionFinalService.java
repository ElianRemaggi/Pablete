package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.request.InscripcionFinalRequest;
import com.instituto.siga.dto.request.ResultadoFinalRequest;
import com.instituto.siga.dto.response.InscripcionFinalResponse;

import java.util.List;

public interface InscripcionFinalService {

    InscripcionFinalResponse inscribir(Long alumnoId, InscripcionFinalRequest request);

    InscripcionFinalResponse modificar(Long alumnoId, Long inscripcionId, InscripcionFinalRequest request);

    void darDeBaja(Long alumnoId, Long inscripcionId);

    List<InscripcionFinalResponse> listarPropias(Long alumnoId);

    List<InscripcionFinalResponse> listarPorMesa(Long mesaId);

    List<InscripcionFinalResponse> listarPorAlumno(Long alumnoId);

    /** Registra el resultado (y nota) de un final rendido (personal autorizado) y actualiza el estado academico. */
    InscripcionFinalResponse registrarResultado(Long inscripcionId, ResultadoFinalRequest request);
}
