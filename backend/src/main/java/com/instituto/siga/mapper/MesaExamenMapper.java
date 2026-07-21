package com.instituto.siga.mapper;

import com.instituto.siga.dto.response.MesaExamenResponse;
import com.instituto.siga.entity.MesaExamen;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MesaExamenMapper {

    @Mapping(source = "materia.id", target = "materiaId")
    @Mapping(source = "materia.nombre", target = "materiaNombre")
    MesaExamenResponse toResponse(MesaExamen entity);
}
