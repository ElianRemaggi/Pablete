package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.request.MesaExamenRequest;
import com.instituto.siga.dto.response.MesaExamenResponse;

import java.time.LocalDate;
import java.util.List;

public interface MesaExamenService {

    List<MesaExamenResponse> listar(Long materiaId, LocalDate fecha);

    MesaExamenResponse crear(MesaExamenRequest request);

    MesaExamenResponse actualizar(Long id, MesaExamenRequest request);

    void eliminar(Long id);
}
