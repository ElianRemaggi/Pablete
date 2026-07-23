package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.request.MateriaRequest;
import com.instituto.siga.dto.request.PlanMateriaRequest;
import com.instituto.siga.dto.response.MateriaResponse;

import java.util.List;

public interface MateriaService {

    List<MateriaResponse> listar(Long carreraId, Long planId, Integer anio);

    MateriaResponse obtenerPorId(Long id);

    MateriaResponse crear(MateriaRequest request);

    MateriaResponse actualizar(Long id, MateriaRequest request);

    void darDeBaja(Long id);

    void asociarAPlan(Long materiaId, Long planId, PlanMateriaRequest request);
}
