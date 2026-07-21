package com.instituto.siga.dto.request;

import jakarta.validation.constraints.Email;

public record UsuarioUpdateRequest(
        @Email String email,
        String nombre,
        String apellido,
        Boolean activo) {
}
