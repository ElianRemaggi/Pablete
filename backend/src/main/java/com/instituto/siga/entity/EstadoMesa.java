package com.instituto.siga.entity;

/**
 * Ciclo de vida de una mesa de examen final. Solo PUBLICADA admite inscripciones nuevas;
 * CERRADA y CANCELADA conservan el historial (no se borra la mesa ni sus inscripciones).
 */
public enum EstadoMesa {
    BORRADOR,
    PUBLICADA,
    CERRADA,
    CANCELADA
}
