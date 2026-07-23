package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.response.CertificadoResponse;

import java.util.List;

public interface CertificadoService {

    byte[] generarAlumnoRegularPropio(Long usuarioId);

    byte[] generarAnaliticoParcialPropio(Long usuarioId);

    byte[] generarAlumnoRegularParaAlumno(Long alumnoId);

    List<CertificadoResponse> historialPropio(Long usuarioId);
}
