package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.request.EventoAcademicoRequest;
import com.instituto.siga.dto.response.EventoAcademicoResponse;

import java.util.List;

public interface EventoAcademicoService {

    List<EventoAcademicoResponse> listar();

    EventoAcademicoResponse crear(EventoAcademicoRequest request);

    EventoAcademicoResponse actualizar(Long id, EventoAcademicoRequest request);

    void eliminar(Long id);
}
