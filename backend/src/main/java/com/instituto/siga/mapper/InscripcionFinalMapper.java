package com.instituto.siga.mapper;

import com.instituto.siga.dto.response.InscripcionFinalResponse;
import com.instituto.siga.entity.InscripcionFinal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InscripcionFinalMapper {

    @Mapping(source = "mesa.materia.nombre", target = "materiaNombre")
    @Mapping(source = "mesa.fecha", target = "fechaMesa")
    @Mapping(source = "mesa.turno", target = "turno")
    @Mapping(source = "mesa.profesor", target = "profesor")
    @Mapping(target = "carreraNombre", ignore = true)
    @Mapping(target = "resultado", ignore = true)
    InscripcionFinalResponse toResponse(InscripcionFinal entity);
}
