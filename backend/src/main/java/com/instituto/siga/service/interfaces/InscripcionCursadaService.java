package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.request.InscripcionCursadaRequest;
import com.instituto.siga.dto.response.InscripcionCursadaResponse;

import java.util.List;

public interface InscripcionCursadaService {

    InscripcionCursadaResponse inscribir(Long alumnoId, InscripcionCursadaRequest request);

    InscripcionCursadaResponse modificar(Long alumnoId, Long inscripcionId, InscripcionCursadaRequest request);

    void darDeBaja(Long alumnoId, Long inscripcionId);

    List<InscripcionCursadaResponse> listarPropias(Long alumnoId);

    List<InscripcionCursadaResponse> listarPorComision(Long comisionId);
}
