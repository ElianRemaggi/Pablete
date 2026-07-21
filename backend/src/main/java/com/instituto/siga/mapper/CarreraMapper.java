package com.instituto.siga.mapper;

import com.instituto.siga.dto.request.CarreraRequest;
import com.instituto.siga.dto.response.CarreraResponse;
import com.instituto.siga.entity.Carrera;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CarreraMapper {

    CarreraResponse toResponse(Carrera entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activa", ignore = true)
    Carrera toEntity(CarreraRequest request);
}
