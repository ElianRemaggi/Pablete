package com.instituto.siga.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.Set;

public record UsuarioRequest(
        @NotBlank String dni,
        @Email @NotBlank String email,
        @NotBlank String nombre,
        @NotBlank String apellido,
        @NotBlank
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Debe tener al menos 8 caracteres, una mayúscula y un número")
        String password,
        @NotEmpty(message = "El usuario debe tener al menos un rol asignado")
        Set<String> roles) {
}
