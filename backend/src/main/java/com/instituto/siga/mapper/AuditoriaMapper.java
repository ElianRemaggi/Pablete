package com.instituto.siga.mapper;

import com.instituto.siga.dto.response.AuditoriaResponse;
import com.instituto.siga.entity.Auditoria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditoriaMapper {

    @Mapping(source = "usuario.id", target = "usuarioId")
    @Mapping(source = "usuario.nombre", target = "usuarioNombre")
    AuditoriaResponse toResponse(Auditoria entity);
}
