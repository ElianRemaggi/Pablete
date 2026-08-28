package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.response.EventoCalendarioResponse;

import java.time.LocalDate;
import java.util.List;

public interface CalendarioService {

    List<EventoCalendarioResponse> calendarioPropio(Long alumnoId, LocalDate desde, LocalDate hasta);

    List<EventoCalendarioResponse> calendarioDeAlumno(Long alumnoId, LocalDate desde, LocalDate hasta);

    List<EventoCalendarioResponse> calendarioInstitucional(LocalDate desde, LocalDate hasta);
}
