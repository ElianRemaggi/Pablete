package com.instituto.siga.mapper;

import com.instituto.siga.dto.request.HorarioRequest;
import com.instituto.siga.dto.response.HorarioResponse;
import com.instituto.siga.entity.Horario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HorarioMapper {

    HorarioResponse toResponse(Horario entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "comision", ignore = true)
    Horario toEntity(HorarioRequest request);
}
