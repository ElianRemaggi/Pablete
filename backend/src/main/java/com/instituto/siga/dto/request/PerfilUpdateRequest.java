package com.instituto.siga.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Unico dato personal que el propio usuario puede cambiar: DNI, nombre, apellido y roles son datos
 * institucionales y solo los modifica un administrador. */
public record PerfilUpdateRequest(@Email @NotBlank @Size(max = 150) String email) {
}
