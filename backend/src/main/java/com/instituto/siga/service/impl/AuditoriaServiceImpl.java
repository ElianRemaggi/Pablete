package com.instituto.siga.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituto.siga.audit.AuditoriaEvent;
import com.instituto.siga.dto.response.AuditoriaResponse;
import com.instituto.siga.entity.Auditoria;
import com.instituto.siga.entity.Usuario;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.AuditoriaMapper;
import com.instituto.siga.repository.AuditoriaRepository;
import com.instituto.siga.repository.UsuarioRepository;
import com.instituto.siga.service.interfaces.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditoriaServiceImpl implements AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaServiceImpl.class);

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaMapper auditoriaMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void registrar(AuditoriaEvent event) {
        Usuario usuario = event.usuarioId() == null ? null : usuarioRepository.findById(event.usuarioId()).orElse(null);

        String detalleJson = null;
        if (event.detalle() != null && !event.detalle().isEmpty()) {
            try {
                detalleJson = objectMapper.writeValueAsString(event.detalle());
            } catch (JsonProcessingException e) {
                log.warn("No se pudo serializar el detalle de auditoria para la accion {}", event.accion(), e);
            }
        }

        Auditoria auditoria = Auditoria.builder()
                .usuario(usuario)
                .accion(event.accion())
                .entidad(event.entidad())
                .entidadId(event.entidadId())
                .detalle(detalleJson)
                .ip(event.ip())
                .fecha(event.fecha())
                .build();

        auditoriaRepository.save(auditoria);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public Page<AuditoriaResponse> buscar(Long usuarioId, String accion, String entidad,
                                           LocalDateTime fechaDesde, LocalDateTime fechaHasta, Pageable pageable) {
        return auditoriaRepository.buscar(usuarioId, accion, entidad, fechaDesde, fechaHasta, pageable)
                .map(auditoriaMapper::toResponse);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public AuditoriaResponse obtenerPorId(Long id) {
        Auditoria auditoria = auditoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de auditoría no encontrado: " + id));
        return auditoriaMapper.toResponse(auditoria);
    }
}
