package com.instituto.siga.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

public record UsuarioResponse(
        Long id,
        String dni,
        String email,
        String nombre,
        String apellido,
        boolean activo,
        Set<String> roles,
        LocalDateTime fechaCreacion) {
}
