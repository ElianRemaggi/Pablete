package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.request.PlanEstudioRequest;
import com.instituto.siga.dto.response.PlanEstudioResponse;

import java.util.List;

public interface PlanEstudioService {

    List<PlanEstudioResponse> listarPorCarrera(Long carreraId);

    PlanEstudioResponse crear(Long carreraId, PlanEstudioRequest request);

    PlanEstudioResponse actualizar(Long id, PlanEstudioRequest request);
}
