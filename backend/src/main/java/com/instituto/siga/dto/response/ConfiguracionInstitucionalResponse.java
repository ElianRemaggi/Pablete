package com.instituto.siga.dto.response;

import java.time.LocalDateTime;

public record ConfiguracionInstitucionalResponse(
        LocalDateTime fechaAperturaCursadas,
        LocalDateTime fechaCierreCursadas,
        LocalDateTime fechaAperturaFinales,
        LocalDateTime fechaCierreFinales,
        int limiteIntentosFinal,
        int horasBloqueoFinal) {
}
