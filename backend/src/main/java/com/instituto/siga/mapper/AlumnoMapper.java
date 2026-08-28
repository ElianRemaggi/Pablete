package com.instituto.siga.mapper;

import com.instituto.siga.dto.response.AlumnoResponse;
import com.instituto.siga.entity.Alumno;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AlumnoMapper {

    @Mapping(source = "usuario.dni", target = "dni")
    @Mapping(source = "usuario.nombre", target = "nombre")
    @Mapping(source = "usuario.apellido", target = "apellido")
    @Mapping(source = "usuario.email", target = "email")
    AlumnoResponse toResponse(Alumno entity);
}
