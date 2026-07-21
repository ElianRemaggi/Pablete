package com.instituto.siga.mapper;

import com.instituto.siga.dto.request.MateriaRequest;
import com.instituto.siga.dto.response.MateriaResponse;
import com.instituto.siga.entity.Materia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MateriaMapper {

    MateriaResponse toResponse(Materia entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activa", ignore = true)
    Materia toEntity(MateriaRequest request);
}
