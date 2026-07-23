package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.request.InscripcionFinalRequest;
import com.instituto.siga.dto.response.InscripcionFinalResponse;

import java.util.List;

public interface InscripcionFinalService {

    InscripcionFinalResponse inscribir(Long alumnoId, InscripcionFinalRequest request);

    InscripcionFinalResponse modificar(Long alumnoId, Long inscripcionId, InscripcionFinalRequest request);

    void darDeBaja(Long alumnoId, Long inscripcionId);

    List<InscripcionFinalResponse> listarPropias(Long alumnoId);

    List<InscripcionFinalResponse> listarPorMesa(Long mesaId);
}
