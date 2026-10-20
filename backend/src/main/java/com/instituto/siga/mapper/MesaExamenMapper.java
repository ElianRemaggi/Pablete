package com.instituto.siga.mapper;

import com.instituto.siga.dto.response.MesaExamenResponse;
import com.instituto.siga.entity.MesaExamen;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MesaExamenMapper {

    /** inscriptos / cupoDisponible / inscripcionAbierta dependen de datos que no estan en la entidad:
     * los completa MesaExamenServiceImpl. */
    @Mapping(source = "materia.id", target = "materiaId")
    @Mapping(source = "materia.nombre", target = "materiaNombre")
    @Mapping(target = "inscriptos", ignore = true)
    @Mapping(target = "cupoDisponible", ignore = true)
    @Mapping(target = "inscripcionAbierta", ignore = true)
    MesaExamenResponse toResponse(MesaExamen entity);
}
