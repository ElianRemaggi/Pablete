package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.request.ComisionRequest;
import com.instituto.siga.dto.request.HorarioRequest;
import com.instituto.siga.dto.response.ComisionResponse;

import java.util.List;

public interface ComisionService {

    List<ComisionResponse> listar(Long materiaId, String periodoLectivo);

    ComisionResponse obtenerPorId(Long id);

    ComisionResponse crear(ComisionRequest request);

    ComisionResponse actualizar(Long id, ComisionRequest request);

    void eliminar(Long id);

    ComisionResponse agregarHorario(Long comisionId, HorarioRequest request);

    ComisionResponse quitarHorario(Long comisionId, Long horarioId);
}
