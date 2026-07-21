package com.instituto.siga.mapper;

import com.instituto.siga.dto.response.ConfiguracionInstitucionalResponse;
import com.instituto.siga.entity.ConfiguracionInstitucional;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConfiguracionInstitucionalMapper {

    ConfiguracionInstitucionalResponse toResponse(ConfiguracionInstitucional entity);
}
