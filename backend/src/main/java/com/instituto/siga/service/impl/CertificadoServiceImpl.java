package com.instituto.siga.service.impl;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.certificado.CertificadoFactory;
import com.instituto.siga.dto.response.CertificadoResponse;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.CertificadoEmitido;
import com.instituto.siga.entity.EstadoAcademicoMateria;
import com.instituto.siga.entity.TipoCertificado;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.CertificadoEmitidoRepository;
import com.instituto.siga.repository.EstadoAcademicoMateriaRepository;
import com.instituto.siga.security.SecurityUtils;
import com.instituto.siga.service.interfaces.CertificadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CertificadoServiceImpl implements CertificadoService {

    private final AlumnoRepository alumnoRepository;
    private final EstadoAcademicoMateriaRepository estadoAcademicoMateriaRepository;
    private final CertificadoEmitidoRepository certificadoEmitidoRepository;
    private final CertificadoFactory certificadoFactory;
    private final AuditoriaPublisher auditoriaPublisher;

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional
    public byte[] generarAlumnoRegularPropio(Long usuarioId) {
        Alumno alumno = buscarOFallar(usuarioId);
        byte[] pdf = certificadoFactory.generarAlumnoRegular(alumno);
        registrar(alumno, TipoCertificado.ALUMNO_REGULAR, pdf);
        return pdf;
    }

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional
    public byte[] generarAnaliticoParcialPropio(Long usuarioId) {
        Alumno alumno = buscarOFallar(usuarioId);
        List<EstadoAcademicoMateria> historial = estadoAcademicoMateriaRepository.findByAlumnoId(usuarioId);
        byte[] pdf = certificadoFactory.generarAnaliticoParcial(alumno, historial);
        registrar(alumno, TipoCertificado.ANALITICO_PARCIAL, pdf);
        return pdf;
    }

    @Override
    @PreAuthorize("hasAnyRole('PRECEPTOR', 'ADMINISTRADOR')")
    @Transactional
    public byte[] generarAlumnoRegularParaAlumno(Long alumnoId) {
        Alumno alumno = buscarOFallar(alumnoId);
        byte[] pdf = certificadoFactory.generarAlumnoRegular(alumno);
        registrar(alumno, TipoCertificado.ALUMNO_REGULAR, pdf);
        return pdf;
    }

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional(readOnly = true)
    public List<CertificadoResponse> historialPropio(Long usuarioId) {
        buscarOFallar(usuarioId);
        return certificadoEmitidoRepository.findByAlumnoId(usuarioId).stream()
                .map(c -> new CertificadoResponse(c.getId(), c.getTipo().name(), c.getFechaEmision(), urlPara(c.getTipo())))
                .toList();
    }

    private String urlPara(TipoCertificado tipo) {
        return tipo == TipoCertificado.ALUMNO_REGULAR
                ? "/api/v1/certificados/alumno-regular"
                : "/api/v1/certificados/analitico-parcial";
    }

    private void registrar(Alumno alumno, TipoCertificado tipo, byte[] pdf) {
        String hash = sha256Hex(pdf);
        CertificadoEmitido guardado = certificadoEmitidoRepository.save(CertificadoEmitido.builder()
                .alumno(alumno).tipo(tipo).fechaEmision(LocalDateTime.now()).hashDocumento(hash).build());
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "CERTIFICADO_DESCARGADO", "CertificadoEmitido",
                guardado.getId(), Map.of("alumnoId", alumno.getUsuarioId(), "tipo", tipo.name(), "hash", hash));
    }

    private String sha256Hex(byte[] datos) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(datos);
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }

    private Alumno buscarOFallar(Long id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado: " + id));
    }
}
