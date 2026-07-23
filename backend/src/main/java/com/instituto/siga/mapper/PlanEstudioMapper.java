package com.instituto.siga.mapper;

import com.instituto.siga.dto.response.PlanEstudioResponse;
import com.instituto.siga.entity.PlanEstudio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlanEstudioMapper {

    @Mapping(source = "carrera.id", target = "carreraId")
    PlanEstudioResponse toResponse(PlanEstudio entity);
}
