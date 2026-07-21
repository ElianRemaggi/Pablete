package com.instituto.siga.service.impl;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.MesaExamenRequest;
import com.instituto.siga.dto.response.MesaExamenResponse;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.MesaExamenMapper;
import com.instituto.siga.repository.MateriaRepository;
import com.instituto.siga.repository.MesaExamenRepository;
import com.instituto.siga.security.SecurityUtils;
import com.instituto.siga.service.interfaces.MesaExamenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Reglas de docs/07-reglas-negocio-validaciones.md SS4.4. La validacion contra el
 * "calendario academico vigente" queda pendiente hasta que exista ConfiguracionInstitucional;
 * por ahora solo se exige que la fecha no sea anterior a hoy.
 */
@Service
@RequiredArgsConstructor
public class MesaExamenServiceImpl implements MesaExamenService {

    private final MesaExamenRepository mesaExamenRepository;
    private final MateriaRepository materiaRepository;
    private final MesaExamenMapper mesaExamenMapper;
    private final AuditoriaPublisher auditoriaPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<MesaExamenResponse> listar(Long materiaId, LocalDate fecha) {
        List<MesaExamen> mesas;
        if (materiaId != null && fecha != null) {
            mesas = mesaExamenRepository.findByMateriaIdAndFecha(materiaId, fecha);
        } else if (materiaId != null) {
            mesas = mesaExamenRepository.findByMateriaId(materiaId);
        } else if (fecha != null) {
            mesas = mesaExamenRepository.findByFecha(fecha);
        } else {
            mesas = mesaExamenRepository.findAll();
        }
        return mesas.stream().map(mesaExamenMapper::toResponse).toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public MesaExamenResponse crear(MesaExamenRequest request) {
        Materia materia = buscarMateriaOFallar(request.materiaId());
        validarFecha(request.fecha());

        if (mesaExamenRepository.existsByMateriaIdAndFechaAndTurno(
                request.materiaId(), request.fecha(), request.turno())) {
            throw new ConflictException("MESA_DUPLICADA",
                    "Ya existe una mesa de esa materia en esa fecha y turno");
        }

        MesaExamen mesa = MesaExamen.builder()
                .materia(materia)
                .fecha(request.fecha())
                .turno(request.turno())
                .cupoMaximo(request.cupoMaximo())
                .build();

        MesaExamen guardada = mesaExamenRepository.save(mesa);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "MESA_EXAMEN_CREADA", "MesaExamen", guardada.getId(),
                Map.of("materiaId", materia.getId(), "fecha", request.fecha().toString(), "turno", request.turno().name()));

        return mesaExamenMapper.toResponse(guardada);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public MesaExamenResponse actualizar(Long id, MesaExamenRequest request) {
        MesaExamen mesa = buscarOFallar(id);
        Materia materia = buscarMateriaOFallar(request.materiaId());
        validarFecha(request.fecha());

        if (mesaExamenRepository.existsByMateriaIdAndFechaAndTurnoAndIdNot(
                request.materiaId(), request.fecha(), request.turno(), id)) {
            throw new ConflictException("MESA_DUPLICADA",
                    "Ya existe una mesa de esa materia en esa fecha y turno");
        }

        mesa.setMateria(materia);
        mesa.setFecha(request.fecha());
        mesa.setTurno(request.turno());
        mesa.setCupoMaximo(request.cupoMaximo());

        MesaExamen guardada = mesaExamenRepository.save(mesa);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "MESA_EXAMEN_ACTUALIZADA", "MesaExamen", guardada.getId(),
                Map.of("materiaId", materia.getId(), "fecha", request.fecha().toString(), "turno", request.turno().name()));

        return mesaExamenMapper.toResponse(guardada);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public void eliminar(Long id) {
        if (!mesaExamenRepository.existsById(id)) {
            throw new ResourceNotFoundException("Mesa de examen no encontrada: " + id);
        }
        mesaExamenRepository.deleteById(id);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "MESA_EXAMEN_ELIMINADA", "MesaExamen", id, Map.of());
    }

    private MesaExamen buscarOFallar(Long id) {
        return mesaExamenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa de examen no encontrada: " + id));
    }

    private Materia buscarMateriaOFallar(Long id) {
        return materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + id));
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha.isBefore(LocalDate.now())) {
            throw new BusinessException("MESA_FUERA_DE_FECHA", "La fecha de la mesa no puede ser anterior a hoy");
        }
    }
}
