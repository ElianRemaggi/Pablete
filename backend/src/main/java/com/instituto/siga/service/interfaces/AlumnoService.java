package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.response.AlumnoResponse;
import com.instituto.siga.dto.response.HistorialAcademicoResponse;

import java.util.List;

public interface AlumnoService {

    AlumnoResponse obtenerPropio(Long usuarioId);

    AlumnoResponse obtenerPorId(Long id);

    List<AlumnoResponse> buscar(String legajo, String dni, String apellido, String nombre);

    List<HistorialAcademicoResponse> historialPropio(Long usuarioId);

    List<HistorialAcademicoResponse> historialPorId(Long id);

    List<HistorialAcademicoResponse> materiasAprobadasPropias(Long usuarioId);

    List<HistorialAcademicoResponse> materiasPendientesPropias(Long usuarioId);
}
