package com.instituto.siga.dto.response;

public record PlanEstudioResponse(
        Long id,
        Long carreraId,
        String version,
        boolean vigente) {
}
