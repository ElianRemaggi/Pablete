package com.instituto.siga.audit;

import com.instituto.siga.util.HttpRequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/** Punto unico que los Services de negocio usan para auditar, sin conocer AuditoriaService ni persistencia. */
@Component
@RequiredArgsConstructor
public class AuditoriaPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publicar(Long usuarioId, String accion, String entidad, Long entidadId, Map<String, Object> detalle) {
        eventPublisher.publishEvent(new AuditoriaEvent(
                usuarioId, accion, entidad, entidadId, detalle, HttpRequestUtils.obtenerIpCliente(), LocalDateTime.now()));
    }
}
