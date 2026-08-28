package com.instituto.siga.dto.response;

import java.time.LocalDate;

public record AlumnoResponse(
        Long usuarioId,
        String dni,
        String nombre,
        String apellido,
        String email,
        String legajo,
        LocalDate fechaIngreso) {
}
