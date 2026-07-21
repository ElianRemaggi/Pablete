package com.instituto.siga.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UsuarioRolesRequest(
        @NotEmpty(message = "El usuario debe tener al menos un rol asignado")
        Set<String> roles) {
}
