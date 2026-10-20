package com.instituto.siga.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Autoregistro de alumnos. No lleva roles: el rol es siempre ALUMNO (nadie se asigna ADMINISTRADOR/PRECEPTOR). */
public record RegistroRequest(
        @NotBlank
        @Pattern(regexp = "^\\d{7,9}$", message = "El DNI debe tener entre 7 y 9 dígitos, sin puntos")
        String dni,
        @Email @NotBlank @Size(max = 150) String email,
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellido,
        @NotBlank
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Debe tener al menos 8 caracteres, una mayúscula y un número")
        String password) {
}
