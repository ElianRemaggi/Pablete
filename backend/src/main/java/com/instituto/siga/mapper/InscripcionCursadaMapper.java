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
    @Mapping(source = "comision.materia.id", target = "materiaId")
    @Mapping(source = "comision.id", target = "comisionId")
    @Mapping(source = "comision.periodoLectivo", target = "periodo")
    @Mapping(source = "alumno.usuarioId", target = "alumnoId")
    @Mapping(source = "alumno.usuario.dni", target = "alumnoDni")
    @Mapping(source = "alumno.usuario.nombre", target = "alumnoNombre")
    @Mapping(source = "alumno.usuario.apellido", target = "alumnoApellido")
    @Mapping(source = "alumno.legajo", target = "alumnoLegajo")
    @Mapping(source = "resultadoCursada", target = "resultadoCursada", defaultValue = "PENDIENTE")
    @Mapping(target = "carreraNombre", ignore = true)
    InscripcionCursadaResponse toResponse(InscripcionCursada entity);
}
