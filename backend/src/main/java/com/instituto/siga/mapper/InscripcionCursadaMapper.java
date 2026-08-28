package com.instituto.siga.mapper;

import com.instituto.siga.dto.response.InscripcionCursadaResponse;
import com.instituto.siga.entity.InscripcionCursada;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = HorarioMapper.class)
public interface InscripcionCursadaMapper {

    @Mapping(source = "comision.materia.nombre", target = "materiaNombre")
    @Mapping(source = "comision.nombre", target = "comisionNombre")
    @Mapping(source = "comision.profesor", target = "profesor")
    @Mapping(source = "comision.horarios", target = "horarios")
    @Mapping(target = "carreraNombre", ignore = true)
    InscripcionCursadaResponse toResponse(InscripcionCursada entity);
}
