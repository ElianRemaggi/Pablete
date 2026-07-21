package com.instituto.siga.service.impl;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.CarreraRequest;
import com.instituto.siga.dto.response.CarreraResponse;
import com.instituto.siga.entity.Carrera;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.CarreraMapper;
import com.instituto.siga.repository.CarreraRepository;
import com.instituto.siga.security.SecurityUtils;
import com.instituto.siga.service.interfaces.CarreraService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CarreraServiceImpl implements CarreraService {

    private final CarreraRepository carreraRepository;
    private final CarreraMapper carreraMapper;
    private final AuditoriaPublisher auditoriaPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<CarreraResponse> listar() {
        return carreraRepository.findAll().stream().map(carreraMapper::toResponse).toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public CarreraResponse crear(CarreraRequest request) {
        if (carreraRepository.existsByCodigo(request.codigo()) || carreraRepository.existsByNombre(request.nombre())) {
            throw new ConflictException("CARRERA_DUPLICADA", "Ya existe una carrera con ese código o nombre");
        }

        Carrera carrera = carreraMapper.toEntity(request);
        carrera.setActiva(true);

        Carrera guardada = carreraRepository.save(carrera);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "CARRERA_CREADA", "Carrera", guardada.getId(),
                Map.of("codigo", guardada.getCodigo(), "nombre", guardada.getNombre()));

        return carreraMapper.toResponse(guardada);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public CarreraResponse actualizar(Long id, CarreraRequest request) {
        Carrera carrera = buscarOFallar(id);
        if (carreraRepository.existsByCodigoAndIdNot(request.codigo(), id)
                || carreraRepository.existsByNombreAndIdNot(request.nombre(), id)) {
            throw new ConflictException("CARRERA_DUPLICADA", "Ya existe una carrera con ese código o nombre");
        }

        carrera.setCodigo(request.codigo());
        carrera.setNombre(request.nombre());

        Carrera guardada = carreraRepository.save(carrera);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "CARRERA_ACTUALIZADA", "Carrera", guardada.getId(),
                Map.of("codigo", guardada.getCodigo(), "nombre", guardada.getNombre()));

        return carreraMapper.toResponse(guardada);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public void darDeBaja(Long id) {
        Carrera carrera = buscarOFallar(id);
        carrera.setActiva(false);
        Carrera guardada = carreraRepository.save(carrera);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "CARRERA_BAJA", "Carrera", guardada.getId(), Map.of());
    }

    private Carrera buscarOFallar(Long id) {
        return carreraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carrera no encontrada: " + id));
    }
}
