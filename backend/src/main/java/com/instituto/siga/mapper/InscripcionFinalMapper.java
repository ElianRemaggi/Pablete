package com.instituto.siga.mapper;

import com.instituto.siga.dto.response.InscripcionFinalResponse;
import com.instituto.siga.entity.InscripcionFinal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InscripcionFinalMapper {

    @Mapping(source = "mesa.id", target = "mesaId")
    @Mapping(source = "mesa.materia.nombre", target = "materiaNombre")
    @Mapping(source = "mesa.fecha", target = "fechaMesa")
    @Mapping(source = "mesa.turno", target = "turno")
    @Mapping(source = "mesa.profesor", target = "profesor")
    @Mapping(source = "mesa.hora", target = "hora")
    @Mapping(source = "mesa.periodo", target = "periodo")
    @Mapping(source = "mesa.aula", target = "aula")
    @Mapping(source = "mesa.modalidad", target = "modalidad")
    @Mapping(source = "mesa.estado", target = "mesaEstado")
    @Mapping(source = "alumno.usuarioId", target = "alumnoId")
    @Mapping(source = "alumno.usuario.dni", target = "alumnoDni")
    @Mapping(source = "alumno.usuario.nombre", target = "alumnoNombre")
    @Mapping(source = "alumno.usuario.apellido", target = "alumnoApellido")
    @Mapping(source = "alumno.legajo", target = "alumnoLegajo")
    @Mapping(target = "carreraNombre", ignore = true)
    @Mapping(target = "resultado", ignore = true)
    @Mapping(target = "nota", ignore = true)
    InscripcionFinalResponse toResponse(InscripcionFinal entity);
}
