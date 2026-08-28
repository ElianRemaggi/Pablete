package com.instituto.siga.mapper;

import com.instituto.siga.dto.request.EventoAcademicoRequest;
import com.instituto.siga.dto.response.EventoAcademicoResponse;
import com.instituto.siga.entity.EventoAcademico;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventoAcademicoMapper {

    EventoAcademicoResponse toResponse(EventoAcademico entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creadoPor", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    EventoAcademico toEntity(EventoAcademicoRequest request);
}
