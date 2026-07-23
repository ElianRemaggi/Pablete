package com.instituto.siga.mapper;

import com.instituto.siga.dto.response.HistorialAcademicoResponse;
import com.instituto.siga.entity.EstadoAcademicoMateria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HistorialAcademicoMapper {

    @Mapping(source = "materia.codigo", target = "materiaCodigo")
    @Mapping(source = "materia.nombre", target = "materiaNombre")
    @Mapping(source = "estado", target = "estado")
    HistorialAcademicoResponse toResponse(EstadoAcademicoMateria entity);
}
