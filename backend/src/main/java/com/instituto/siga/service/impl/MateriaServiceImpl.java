package com.instituto.siga.service.impl;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.MateriaRequest;
import com.instituto.siga.dto.request.PlanMateriaRequest;
import com.instituto.siga.dto.response.MateriaResponse;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.PlanEstudio;
import com.instituto.siga.entity.PlanMateria;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.MateriaMapper;
import com.instituto.siga.repository.MateriaRepository;
import com.instituto.siga.repository.PlanEstudioRepository;
import com.instituto.siga.repository.PlanMateriaRepository;
import com.instituto.siga.security.SecurityUtils;
import com.instituto.siga.service.interfaces.MateriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MateriaServiceImpl implements MateriaService {

    private final MateriaRepository materiaRepository;
    private final PlanEstudioRepository planEstudioRepository;
    private final PlanMateriaRepository planMateriaRepository;
    private final MateriaMapper materiaMapper;
    private final AuditoriaPublisher auditoriaPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<MateriaResponse> listar(Long carreraId, Long planId, Integer anio) {
        return materiaRepository.buscar(carreraId, planId, anio).stream().map(materiaMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MateriaResponse obtenerPorId(Long id) {
        return materiaMapper.toResponse(buscarOFallar(id));
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public MateriaResponse crear(MateriaRequest request) {
        if (materiaRepository.existsByCodigo(request.codigo())) {
            throw new ConflictException("MATERIA_DUPLICADA", "Ya existe una materia con ese código");
        }

        Materia materia = materiaMapper.toEntity(request);
        materia.setActiva(true);

        Materia guardada = materiaRepository.save(materia);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "MATERIA_CREADA", "Materia", guardada.getId(),
                Map.of("codigo", guardada.getCodigo(), "nombre", guardada.getNombre()));

        return materiaMapper.toResponse(guardada);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public MateriaResponse actualizar(Long id, MateriaRequest request) {
        Materia materia = buscarOFallar(id);
        if (materiaRepository.existsByCodigoAndIdNot(request.codigo(), id)) {
            throw new ConflictException("MATERIA_DUPLICADA", "Ya existe una materia con ese código");
        }

        materia.setCodigo(request.codigo());
        materia.setNombre(request.nombre());
        materia.setCargaHoraria(request.cargaHoraria());

        Materia guardada = materiaRepository.save(materia);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "MATERIA_ACTUALIZADA", "Materia", guardada.getId(),
                Map.of("codigo", guardada.getCodigo(), "nombre", guardada.getNombre()));

        return materiaMapper.toResponse(guardada);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public void darDeBaja(Long id) {
        Materia materia = buscarOFallar(id);
        materia.setActiva(false);
        Materia guardada = materiaRepository.save(materia);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "MATERIA_BAJA", "Materia", guardada.getId(), Map.of());
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public void asociarAPlan(Long materiaId, Long planId, PlanMateriaRequest request) {
        Materia materia = buscarOFallar(materiaId);
        PlanEstudio plan = planEstudioRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de estudio no encontrado: " + planId));

        if (planMateriaRepository.existsByPlanIdAndMateriaId(planId, materiaId)) {
            throw new ConflictException("MATERIA_YA_ASOCIADA_AL_PLAN", "Esa materia ya está asociada a este plan");
        }

        PlanMateria planMateria = PlanMateria.builder()
                .plan(plan).materia(materia).anio(request.anio()).cuatrimestre(request.cuatrimestre())
                .build();
        planMateriaRepository.save(planMateria);

        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "MATERIA_ASOCIADA_A_PLAN", "PlanMateria", planId,
                Map.of("materiaId", materiaId, "anio", request.anio(), "cuatrimestre", request.cuatrimestre()));
    }

    private Materia buscarOFallar(Long id) {
        return materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + id));
    }
}
