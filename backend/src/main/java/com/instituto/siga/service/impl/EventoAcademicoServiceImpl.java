package com.instituto.siga.service.impl;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.EventoAcademicoRequest;
import com.instituto.siga.dto.response.EventoAcademicoResponse;
import com.instituto.siga.entity.EventoAcademico;
import com.instituto.siga.entity.Usuario;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.EventoAcademicoMapper;
import com.instituto.siga.repository.EventoAcademicoRepository;
import com.instituto.siga.repository.UsuarioRepository;
import com.instituto.siga.security.SecurityUtils;
import com.instituto.siga.service.interfaces.EventoAcademicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventoAcademicoServiceImpl implements EventoAcademicoService {

    private final EventoAcademicoRepository eventoAcademicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EventoAcademicoMapper eventoAcademicoMapper;
    private final AuditoriaPublisher auditoriaPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<EventoAcademicoResponse> listar() {
        return eventoAcademicoRepository.findAll().stream().map(eventoAcademicoMapper::toResponse).toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public EventoAcademicoResponse crear(EventoAcademicoRequest request) {
        EventoAcademico evento = eventoAcademicoMapper.toEntity(request);
        evento.setFechaCreacion(LocalDateTime.now());
        Long usuarioActualId = SecurityUtils.usuarioActualId();
        if (usuarioActualId != null) {
            Usuario creador = usuarioRepository.findById(usuarioActualId).orElse(null);
            evento.setCreadoPor(creador);
        }

        EventoAcademico guardado = eventoAcademicoRepository.save(evento);
        auditoriaPublisher.publicar(usuarioActualId, "EVENTO_ACADEMICO_CREADO", "EventoAcademico", guardado.getId(),
                Map.of("titulo", guardado.getTitulo(), "tipo", guardado.getTipo().name()));

        return eventoAcademicoMapper.toResponse(guardado);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public EventoAcademicoResponse actualizar(Long id, EventoAcademicoRequest request) {
        EventoAcademico evento = buscarOFallar(id);
        evento.setTitulo(request.titulo());
        evento.setDescripcion(request.descripcion());
        evento.setTipo(request.tipo());
        evento.setFechaInicio(request.fechaInicio());
        evento.setFechaFin(request.fechaFin());

        EventoAcademico guardado = eventoAcademicoRepository.save(evento);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "EVENTO_ACADEMICO_ACTUALIZADO", "EventoAcademico",
                guardado.getId(), Map.of("titulo", guardado.getTitulo(), "tipo", guardado.getTipo().name()));

        return eventoAcademicoMapper.toResponse(guardado);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public void eliminar(Long id) {
        if (!eventoAcademicoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Evento académico no encontrado: " + id);
        }
        eventoAcademicoRepository.deleteById(id);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "EVENTO_ACADEMICO_ELIMINADO", "EventoAcademico", id,
                Map.of());
    }

    private EventoAcademico buscarOFallar(Long id) {
        return eventoAcademicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento académico no encontrado: " + id));
    }
}
