package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.response.TasaAprobacionResponse;

import java.util.List;

public interface ReporteService {

    List<TasaAprobacionResponse> tasaAprobacionPorMateria();
}
