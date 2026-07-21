package com.instituto.siga.mapper;

import com.instituto.siga.dto.request.UsuarioRequest;
import com.instituto.siga.dto.response.UsuarioResponse;
import com.instituto.siga.entity.Rol;
import com.instituto.siga.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapeo declarativo puro (docs/03 SS4): el password hasheado y la resolucion de
 * roles (String -> Rol) son logica de negocio y quedan a cargo de UsuarioServiceImpl.
 */
@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "roles", expression = "java(mapRoles(entity.getRoles()))")
    UsuarioResponse toResponse(Usuario entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    Usuario toEntity(UsuarioRequest request);

    default Set<String> mapRoles(Set<Rol> roles) {
        return roles.stream().map(Rol::getNombre).collect(Collectors.toSet());
    }
}
