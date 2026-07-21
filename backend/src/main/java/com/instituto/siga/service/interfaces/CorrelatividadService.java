package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.request.CorrelatividadRequest;
import com.instituto.siga.dto.response.CorrelatividadResponse;

import java.util.List;

public interface CorrelatividadService {

    List<CorrelatividadResponse> listarPorMateria(Long materiaId);

    CorrelatividadResponse crear(CorrelatividadRequest request);

    void eliminar(Long id);
}
