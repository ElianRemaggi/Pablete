package com.instituto.siga.dto.response;

import com.instituto.siga.entity.TipoEventoCalendario;

import java.time.LocalDateTime;

/**
 * Evento del calendario derivado (docs/04, seccion Calendario). El id es String porque los
 * eventos derivados de horario/mesa/inscripcion/configuracion llevan ids sinteticos
 * (ej. "horario-12-2026-04-06"), no un id de fila propio.
 */
public record EventoCalendarioResponse(
        String id,
        String titulo,
        String detalle,
        LocalDateTime inicio,
        LocalDateTime fin,
        TipoEventoCalendario tipo,
        String ubicacion) {
}
