package com.instituto.siga.audit;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Evento generico unico para toda accion auditable (docs/08-auditoria.md SS4), en vez de una
 * clase de evento distinta por accion — mismo desacople (Service no conoce como se audita),
 * sin multiplicar clases estructuralmente identicas.
 *
 * {@code fecha} se estampa en AuditoriaPublisher (hilo sincrono, en el momento real de la
 * accion), no en el listener asincrono, para que el timestamp no dependa de cuando el
 * scheduler de @Async llega a ejecutarlo.
 */
public record AuditoriaEvent(
        Long usuarioId,
        String accion,
        String entidad,
        Long entidadId,
        Map<String, Object> detalle,
        String ip,
        LocalDateTime fecha) {
}
