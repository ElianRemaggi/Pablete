package com.instituto.siga.service.impl;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.CorrelatividadRequest;
import com.instituto.siga.dto.response.CorrelatividadResponse;
import com.instituto.siga.entity.Correlatividad;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.CorrelatividadMapper;
import com.instituto.siga.repository.CorrelatividadRepository;
import com.instituto.siga.repository.MateriaRepository;
import com.instituto.siga.security.SecurityUtils;
import com.instituto.siga.service.interfaces.CorrelatividadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reglas de docs/07-reglas-negocio-validaciones.md SS4.2: sin autoreferencia, sin duplicados, sin ciclos. */
@Service
@RequiredArgsConstructor
public class CorrelatividadServiceImpl implements CorrelatividadService {

    private final CorrelatividadRepository correlatividadRepository;
    private final MateriaRepository materiaRepository;
    private final CorrelatividadMapper correlatividadMapper;
    private final AuditoriaPublisher auditoriaPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<CorrelatividadResponse> listarPorMateria(Long materiaId) {
        return correlatividadRepository.findByMateriaId(materiaId).stream()
                .map(correlatividadMapper::toResponse)
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public CorrelatividadResponse crear(CorrelatividadRequest request) {
        if (request.materiaId().equals(request.materiaRequeridaId())) {
            throw new BusinessException("CORRELATIVIDAD_AUTOREFERENCIA",
                    "Una materia no puede ser correlativa de sí misma");
        }

        Materia materia = buscarMateriaOFallar(request.materiaId());
        Materia materiaRequerida = buscarMateriaOFallar(request.materiaRequeridaId());

        if (correlatividadRepository.existsByMateriaIdAndMateriaRequeridaIdAndTipoRequisito(
                request.materiaId(), request.materiaRequeridaId(), request.tipoRequisito())) {
            throw new ConflictException("CORRELATIVIDAD_DUPLICADA", "Ya existe esa correlatividad");
        }

        if (creariaCiclo(request.materiaId(), request.materiaRequeridaId())) {
            throw new BusinessException("CORRELATIVIDAD_CICLICA",
                    "Esta correlatividad generaría un ciclo con las ya existentes");
        }

        Correlatividad correlatividad = Correlatividad.builder()
                .materia(materia)
                .materiaRequerida(materiaRequerida)
                .tipoRequisito(request.tipoRequisito())
                .build();

        Correlatividad guardada = correlatividadRepository.save(correlatividad);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "CORRELATIVIDAD_CREADA", "Correlatividad",
                guardada.getId(), Map.of("materiaId", materia.getId(), "materiaRequeridaId", materiaRequerida.getId(),
                        "tipoRequisito", request.tipoRequisito().name()));

        return correlatividadMapper.toResponse(guardada);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public void eliminar(Long id) {
        if (!correlatividadRepository.existsById(id)) {
            throw new ResourceNotFoundException("Correlatividad no encontrada: " + id);
        }
        correlatividadRepository.deleteById(id);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "CORRELATIVIDAD_ELIMINADA", "Correlatividad", id, Map.of());
    }

    private Materia buscarMateriaOFallar(Long id) {
        return materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + id));
    }

    /**
     * Una nueva arista materiaId -> materiaRequeridaId cierra un ciclo si materiaRequeridaId
     * ya puede alcanzar a materiaId en el grafo existente (BFS sobre las aristas actuales).
     */
    private boolean creariaCiclo(Long materiaId, Long materiaRequeridaId) {
        Map<Long, List<Long>> grafo = new HashMap<>();
        for (Object[] arista : correlatividadRepository.buscarAristas()) {
            Long desde = (Long) arista[0];
            Long hacia = (Long) arista[1];
            grafo.computeIfAbsent(desde, k -> new ArrayList<>()).add(hacia);
        }

        Deque<Long> pendientes = new ArrayDeque<>();
        Set<Long> visitados = new HashSet<>();
        pendientes.push(materiaRequeridaId);

        while (!pendientes.isEmpty()) {
            Long actual = pendientes.pop();
            if (actual.equals(materiaId)) {
                return true;
            }
            if (!visitados.add(actual)) {
                continue;
            }
            grafo.getOrDefault(actual, List.of()).forEach(pendientes::push);
        }

        return false;
    }
}
