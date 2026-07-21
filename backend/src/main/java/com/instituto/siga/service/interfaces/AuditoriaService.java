package com.instituto.siga.service.interfaces;

import com.instituto.siga.audit.AuditoriaEvent;
import com.instituto.siga.dto.response.AuditoriaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AuditoriaService {

    void registrar(AuditoriaEvent event);

    Page<AuditoriaResponse> buscar(Long usuarioId, String accion, String entidad,
                                    LocalDateTime fechaDesde, LocalDateTime fechaHasta, Pageable pageable);

    AuditoriaResponse obtenerPorId(Long id);
}
