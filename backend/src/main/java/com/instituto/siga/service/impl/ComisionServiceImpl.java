package com.instituto.siga.service.impl;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.ComisionRequest;
import com.instituto.siga.dto.request.HorarioRequest;
import com.instituto.siga.dto.response.ComisionResponse;
import com.instituto.siga.entity.Comision;
import com.instituto.siga.entity.Horario;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.ComisionMapper;
import com.instituto.siga.mapper.HorarioMapper;
import com.instituto.siga.repository.ComisionRepository;
import com.instituto.siga.repository.MateriaRepository;
import com.instituto.siga.security.SecurityUtils;
import com.instituto.siga.service.interfaces.ComisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reglas de docs/07-reglas-negocio-validaciones.md SS4.3 (horarios y comisiones). */
@Service
@RequiredArgsConstructor
public class ComisionServiceImpl implements ComisionService {

    private final ComisionRepository comisionRepository;
    private final MateriaRepository materiaRepository;
    private final ComisionMapper comisionMapper;
    private final HorarioMapper horarioMapper;
    private final AuditoriaPublisher auditoriaPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<ComisionResponse> listar(Long materiaId, String periodoLectivo) {
        List<Comision> comisiones;
        if (materiaId != null && periodoLectivo != null) {
            comisiones = comisionRepository.findByMateriaIdAndPeriodoLectivo(materiaId, periodoLectivo);
        } else if (materiaId != null) {
            comisiones = comisionRepository.findByMateriaId(materiaId);
        } else if (periodoLectivo != null) {
            comisiones = comisionRepository.findByPeriodoLectivo(periodoLectivo);
        } else {
            comisiones = comisionRepository.findAll();
        }
        return comisiones.stream().map(comisionMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ComisionResponse obtenerPorId(Long id) {
        return comisionMapper.toResponse(buscarOFallar(id));
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public ComisionResponse crear(ComisionRequest request) {
        Materia materia = buscarMateriaOFallar(request.materiaId());

        if (comisionRepository.existsByMateriaIdAndNombreAndPeriodoLectivo(
                request.materiaId(), request.nombre(), request.periodoLectivo())) {
            throw new ConflictException("COMISION_DUPLICADA",
                    "Ya existe una comisión con ese nombre para esa materia y período");
        }

        List<HorarioRequest> horariosRequest = request.horarios() == null ? List.of() : request.horarios();
        validarHorariosNuevos(horariosRequest);

        Comision comision = Comision.builder()
                .materia(materia)
                .nombre(request.nombre())
                .periodoLectivo(request.periodoLectivo())
                .cupoMaximo(request.cupoMaximo())
                .profesor(request.profesor())
                .horarios(new ArrayList<>())
                .build();

        for (HorarioRequest hr : horariosRequest) {
            Horario horario = horarioMapper.toEntity(hr);
            horario.setComision(comision);
            comision.getHorarios().add(horario);
        }

        Comision guardada = comisionRepository.save(comision);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "COMISION_CREADA", "Comision", guardada.getId(),
                Map.of("materiaId", materia.getId(), "nombre", guardada.getNombre(), "periodoLectivo", guardada.getPeriodoLectivo()));

        return comisionMapper.toResponse(guardada);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public ComisionResponse actualizar(Long id, ComisionRequest request) {
        Comision comision = buscarOFallar(id);
        Materia materia = buscarMateriaOFallar(request.materiaId());

        boolean cambiaClaveUnica = !comision.getMateria().getId().equals(request.materiaId())
                || !comision.getNombre().equals(request.nombre())
                || !comision.getPeriodoLectivo().equals(request.periodoLectivo());
        if (cambiaClaveUnica && comisionRepository.existsByMateriaIdAndNombreAndPeriodoLectivo(
                request.materiaId(), request.nombre(), request.periodoLectivo())) {
            throw new ConflictException("COMISION_DUPLICADA",
                    "Ya existe una comisión con ese nombre para esa materia y período");
        }

        comision.setMateria(materia);
        comision.setNombre(request.nombre());
        comision.setPeriodoLectivo(request.periodoLectivo());
        comision.setCupoMaximo(request.cupoMaximo());
        comision.setProfesor(request.profesor());

        Comision guardada = comisionRepository.save(comision);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "COMISION_ACTUALIZADA", "Comision", guardada.getId(),
                Map.of("materiaId", materia.getId(), "nombre", guardada.getNombre(), "periodoLectivo", guardada.getPeriodoLectivo()));

        return comisionMapper.toResponse(guardada);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public void eliminar(Long id) {
        if (!comisionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Comisión no encontrada: " + id);
        }
        comisionRepository.deleteById(id);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "COMISION_ELIMINADA", "Comision", id, Map.of());
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public ComisionResponse agregarHorario(Long comisionId, HorarioRequest request) {
        Comision comision = buscarOFallar(comisionId);
        validarHorario(request);
        validarSinSuperposicion(comision.getHorarios(), request);

        Horario horario = horarioMapper.toEntity(request);
        horario.setComision(comision);
        comision.getHorarios().add(horario);

        Comision guardada = comisionRepository.save(comision);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "HORARIO_AGREGADO", "Comision", guardada.getId(),
                Map.of("diaSemana", request.diaSemana(), "horaInicio", request.horaInicio().toString(),
                        "horaFin", request.horaFin().toString()));

        return comisionMapper.toResponse(guardada);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public ComisionResponse quitarHorario(Long comisionId, Long horarioId) {
        Comision comision = buscarOFallar(comisionId);
        boolean eliminado = comision.getHorarios().removeIf(h -> h.getId().equals(horarioId));
        if (!eliminado) {
            throw new ResourceNotFoundException("Horario no encontrado en la comisión: " + horarioId);
        }
        Comision guardada = comisionRepository.save(comision);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "HORARIO_ELIMINADO", "Comision", guardada.getId(),
                Map.of("horarioId", horarioId));
        return comisionMapper.toResponse(guardada);
    }

    private Comision buscarOFallar(Long id) {
        return comisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comisión no encontrada: " + id));
    }

    private Materia buscarMateriaOFallar(Long id) {
        return materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + id));
    }

    private void validarHorario(HorarioRequest h) {
        if (!h.horaFin().isAfter(h.horaInicio())) {
            throw new BusinessException("HORARIO_INVALIDO", "La hora de fin debe ser posterior a la hora de inicio");
        }
    }

    private boolean seSuperponen(HorarioRequest a, HorarioRequest b) {
        return a.diaSemana() == b.diaSemana()
                && a.horaInicio().isBefore(b.horaFin())
                && b.horaInicio().isBefore(a.horaFin());
    }

    /** Valida cada horario nuevo y que no se superpongan entre sí (alta con varios horarios a la vez). */
    private void validarHorariosNuevos(List<HorarioRequest> horarios) {
        for (int i = 0; i < horarios.size(); i++) {
            validarHorario(horarios.get(i));
            for (int j = i + 1; j < horarios.size(); j++) {
                if (seSuperponen(horarios.get(i), horarios.get(j))) {
                    throw new BusinessException("HORARIO_SUPERPUESTO",
                            "Los horarios informados se superponen entre sí");
                }
            }
        }
    }

    private void validarSinSuperposicion(List<Horario> existentes, HorarioRequest nuevo) {
        boolean superpone = existentes.stream().anyMatch(existente ->
                existente.getDiaSemana() == nuevo.diaSemana()
                        && existente.getHoraInicio().isBefore(nuevo.horaFin())
                        && nuevo.horaInicio().isBefore(existente.getHoraFin()));
        if (superpone) {
            throw new BusinessException("HORARIO_SUPERPUESTO",
                    "El horario se superpone con otro ya existente en la comisión");
        }
    }
}
