package com.instituto.siga.dto.response;

import java.time.LocalTime;

public record HorarioResponse(
        Long id,
        int diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        String aula) {
}
