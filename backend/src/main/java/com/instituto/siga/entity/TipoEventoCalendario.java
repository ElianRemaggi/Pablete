package com.instituto.siga.entity;

/**
 * Tipos que puede llevar un evento del calendario derivado (nunca persistido tal cual, ver
 * CalendarioServiceImpl). CURSADA/FINAL/INSCRIPCION se arman en tiempo real a partir de
 * inscripcion_cursada/inscripcion_final/horario/mesa_examen; EVENTO_ACADEMICO y FECHA_LIMITE
 * pueden venir de evento_academico o de configuracion_institucional.
 */
public enum TipoEventoCalendario {
    CURSADA,
    FINAL,
    INSCRIPCION,
    EVENTO_ACADEMICO,
    FECHA_LIMITE
}
