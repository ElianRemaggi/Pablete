package com.instituto.siga.service.impl;

import com.instituto.siga.dto.response.AlumnoResponse;
import com.instituto.siga.dto.response.HistorialAcademicoResponse;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.EstadoMateria;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.AlumnoMapper;
import com.instituto.siga.mapper.HistorialAcademicoMapper;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.EstadoAcademicoMateriaRepository;
import com.instituto.siga.service.interfaces.AlumnoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlumnoServiceImpl implements AlumnoService {

    private final AlumnoRepository alumnoRepository;
    private final EstadoAcademicoMateriaRepository estadoAcademicoMateriaRepository;
    private final AlumnoMapper alumnoMapper;
    private final HistorialAcademicoMapper historialAcademicoMapper;

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional(readOnly = true)
    public AlumnoResponse obtenerPropio(Long usuarioId) {
        return alumnoMapper.toResponse(buscarOFallar(usuarioId));
    }

    @Override
    @PreAuthorize("hasAnyRole('PRECEPTOR', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public AlumnoResponse obtenerPorId(Long id) {
        return alumnoMapper.toResponse(buscarOFallar(id));
    }

    @Override
    @PreAuthorize("hasAnyRole('PRECEPTOR', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public List<AlumnoResponse> buscar(String legajo, String dni, String apellido, String nombre) {
        return alumnoRepository.buscar(legajo, dni, apellido, nombre).stream().map(alumnoMapper::toResponse).toList();
    }

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional(readOnly = true)
    public List<HistorialAcademicoResponse> historialPropio(Long usuarioId) {
        buscarOFallar(usuarioId);
        return historial(usuarioId);
    }

    @Override
    @PreAuthorize("hasAnyRole('PRECEPTOR', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public List<HistorialAcademicoResponse> historialPorId(Long id) {
        buscarOFallar(id);
        return historial(id);
    }

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional(readOnly = true)
    public List<HistorialAcademicoResponse> materiasAprobadasPropias(Long usuarioId) {
        buscarOFallar(usuarioId);
        return historial(usuarioId).stream()
                .filter(h -> EstadoMateria.APROBADA.name().equals(h.estado()))
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional(readOnly = true)
    public List<HistorialAcademicoResponse> materiasPendientesPropias(Long usuarioId) {
        buscarOFallar(usuarioId);
        return historial(usuarioId).stream()
                .filter(h -> !EstadoMateria.APROBADA.name().equals(h.estado()))
                .toList();
    }

    private List<HistorialAcademicoResponse> historial(Long alumnoId) {
        return estadoAcademicoMateriaRepository.findByAlumnoId(alumnoId).stream()
                .map(historialAcademicoMapper::toResponse)
                .toList();
    }

    private Alumno buscarOFallar(Long id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado: " + id));
    }
}
