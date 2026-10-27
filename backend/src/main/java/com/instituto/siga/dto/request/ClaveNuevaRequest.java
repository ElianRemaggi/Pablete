package com.instituto.siga.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Nueva clave definida por el administrador para un usuario (mismas reglas que al crear el usuario). */
public record ClaveNuevaRequest(
        @NotBlank
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Debe tener al menos 8 caracteres, una mayúscula y un número")
        String nuevaClave) {
}
