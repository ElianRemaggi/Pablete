package com.instituto.siga.service.impl;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.PlanEstudioRequest;
import com.instituto.siga.dto.response.PlanEstudioResponse;
import com.instituto.siga.entity.Carrera;
import com.instituto.siga.entity.PlanEstudio;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.PlanEstudioMapper;
import com.instituto.siga.repository.CarreraRepository;
import com.instituto.siga.repository.PlanEstudioRepository;
import com.instituto.siga.security.SecurityUtils;
import com.instituto.siga.service.interfaces.PlanEstudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlanEstudioServiceImpl implements PlanEstudioService {

    private final PlanEstudioRepository planEstudioRepository;
    private final CarreraRepository carreraRepository;
    private final PlanEstudioMapper planEstudioMapper;
    private final AuditoriaPublisher auditoriaPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<PlanEstudioResponse> listarPorCarrera(Long carreraId) {
        return planEstudioRepository.findByCarreraId(carreraId).stream()
                .map(planEstudioMapper::toResponse)
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public PlanEstudioResponse crear(Long carreraId, PlanEstudioRequest request) {
        Carrera carrera = carreraRepository.findById(carreraId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrera no encontrada: " + carreraId));

        if (planEstudioRepository.existsByCarreraIdAndVersion(carreraId, request.version())) {
            throw new ConflictException("PLAN_ESTUDIO_DUPLICADO", "Ya existe un plan con esa versión para esta carrera");
        }

        PlanEstudio plan = PlanEstudio.builder()
                .carrera(carrera)
                .version(request.version())
                .vigente(request.vigente() == null || request.vigente())
                .build();

        PlanEstudio guardado = planEstudioRepository.save(plan);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "PLAN_ESTUDIO_CREADO", "PlanEstudio", guardado.getId(),
                Map.of("carreraId", carreraId, "version", request.version()));

        return planEstudioMapper.toResponse(guardado);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public PlanEstudioResponse actualizar(Long id, PlanEstudioRequest request) {
        PlanEstudio plan = planEstudioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de estudio no encontrado: " + id));

        if (planEstudioRepository.existsByCarreraIdAndVersionAndIdNot(plan.getCarrera().getId(), request.version(), id)) {
            throw new ConflictException("PLAN_ESTUDIO_DUPLICADO", "Ya existe un plan con esa versión para esta carrera");
        }

        plan.setVersion(request.version());
        if (request.vigente() != null) {
            plan.setVigente(request.vigente());
        }

        PlanEstudio guardado = planEstudioRepository.save(plan);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "PLAN_ESTUDIO_ACTUALIZADO", "PlanEstudio", guardado.getId(),
                Map.of("version", guardado.getVersion(), "vigente", guardado.isVigente()));

        return planEstudioMapper.toResponse(guardado);
    }
}
