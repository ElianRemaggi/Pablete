package com.instituto.siga.service.impl;

import com.instituto.siga.dto.response.TasaAprobacionResponse;
import com.instituto.siga.repository.EstadoAcademicoMateriaRepository;
import com.instituto.siga.service.interfaces.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {

    private final EstadoAcademicoMateriaRepository estadoAcademicoMateriaRepository;

    @Override
    @PreAuthorize("hasRole('PRECEPTOR') or hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public List<TasaAprobacionResponse> tasaAprobacionPorMateria() {
        return estadoAcademicoMateriaRepository.calcularTasaAprobacionPorMateria().stream()
                .map(fila -> new TasaAprobacionResponse((Long) fila[0], (String) fila[1], (Long) fila[2], (Long) fila[3]))
                .toList();
    }
}
