package com.instituto.siga.audit;

import com.instituto.siga.service.interfaces.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Persiste de forma asincrona, sin bloquear la respuesta al usuario (docs/08-auditoria.md SS4).
 *
 * AFTER_COMPLETION (no AFTER_COMMIT): eventos como LOGIN_FALLIDO se publican dentro de un
 * metodo @Transactional que a proposito relanza una excepcion despues (para devolver 401),
 * lo que hace ROLLBACK de esa transaccion — con AFTER_COMMIT ese evento nunca se dispararia,
 * perdiendo silenciosamente justo los intentos fallidos que mas interesa auditar. El riesgo
 * a cambio (un evento de alta/baja disparandose aunque esa escritura puntual haga rollback
 * por una falla posterior en el commit) es mucho menos probable y docs SS1 prioriza no perder
 * trazabilidad por sobre la pureza del patron. fallbackExecution=true cubre ademas el caso de
 * publicarse fuera de toda transaccion activa.
 */
@Component
@RequiredArgsConstructor
public class AuditoriaListener {

    private final AuditoriaService auditoriaService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION, fallbackExecution = true)
    public void onAuditoriaEvent(AuditoriaEvent event) {
        auditoriaService.registrar(event);
    }
}
