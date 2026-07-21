package com.instituto.siga.mapper;

import com.instituto.siga.dto.response.CorrelatividadResponse;
import com.instituto.siga.entity.Correlatividad;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CorrelatividadMapper {

    @Mapping(source = "materia.id", target = "materiaId")
    @Mapping(source = "materia.nombre", target = "materiaNombre")
    @Mapping(source = "materiaRequerida.id", target = "materiaRequeridaId")
    @Mapping(source = "materiaRequerida.nombre", target = "materiaRequeridaNombre")
    CorrelatividadResponse toResponse(Correlatividad entity);
}
