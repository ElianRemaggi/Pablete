package com.instituto.siga.service.impl;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.certificado.CertificadoFactory;
import com.instituto.siga.dto.response.CertificadoResponse;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.CertificadoEmitido;
import com.instituto.siga.entity.EstadoAcademicoMateria;
import com.instituto.siga.entity.InscripcionCursada;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.entity.TipoCertificado;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.CertificadoEmitidoRepository;
import com.instituto.siga.repository.EstadoAcademicoMateriaRepository;
import com.instituto.siga.repository.InscripcionCursadaRepository;
import com.instituto.siga.repository.InscripcionFinalRepository;
import com.instituto.siga.repository.PlanMateriaRepository;
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
    private final InscripcionCursadaRepository inscripcionCursadaRepository;
    private final InscripcionFinalRepository inscripcionFinalRepository;
    private final PlanMateriaRepository planMateriaRepository;
    private final CertificadoFactory certificadoFactory;
    private final AuditoriaPublisher auditoriaPublisher;

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional
    public byte[] generarAlumnoRegularPropio(Long usuarioId) {
        Alumno alumno = buscarOFallar(usuarioId);
        byte[] pdf = constanciaAlumnoRegular(alumno);
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
        byte[] pdf = constanciaAlumnoRegular(alumno);
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

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional
    public byte[] generarComprobanteCursadaPropio(Long alumnoId, Long inscripcionId) {
        InscripcionCursada inscripcion = inscripcionCursadaRepository.findByIdAndAlumnoId(inscripcionId, alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada: " + inscripcionId));
        return emitirComprobanteCursada(inscripcion);
    }

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional
    public byte[] generarComprobanteFinalPropio(Long alumnoId, Long inscripcionId) {
        InscripcionFinal inscripcion = inscripcionFinalRepository.findByIdAndAlumnoId(inscripcionId, alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada: " + inscripcionId));
        return emitirComprobanteFinal(inscripcion);
    }

    @Override
    @PreAuthorize("hasAnyRole('PRECEPTOR', 'ADMINISTRADOR')")
    @Transactional
    public byte[] generarComprobanteCursadaParaAlumno(Long alumnoId, Long inscripcionId) {
        InscripcionCursada inscripcion = inscripcionCursadaRepository.findByIdAndAlumnoId(inscripcionId, alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada: " + inscripcionId));
        return emitirComprobanteCursada(inscripcion);
    }

    @Override
    @PreAuthorize("hasAnyRole('PRECEPTOR', 'ADMINISTRADOR')")
    @Transactional
    public byte[] generarComprobanteFinalParaAlumno(Long alumnoId, Long inscripcionId) {
        InscripcionFinal inscripcion = inscripcionFinalRepository.findByIdAndAlumnoId(inscripcionId, alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada: " + inscripcionId));
        return emitirComprobanteFinal(inscripcion);
    }

    /** La constancia es solo el formato institucional en blanco: el alumno o la persona autorizada la completa a mano.
     * Por eso no se evalua ninguna regla de regularidad ni se cargan datos del alumno en el documento. */
    private byte[] constanciaAlumnoRegular(Alumno alumno) {
        return certificadoFactory.generarConstanciaAlumnoRegular();
    }

    private byte[] emitirComprobanteCursada(InscripcionCursada inscripcion) {
        Alumno alumno = inscripcion.getAlumno();
        Materia materia = inscripcion.getComision().getMateria();
        String numero = "CUR-" + String.format("%06d", inscripcion.getId()) + "-" + inscripcion.getFechaInscripcion().getYear();

        byte[] pdf = certificadoFactory.generarComprobanteCursada(alumno, numero, resolverCarrera(materia),
                materia.getNombre(), inscripcion.getComision().getNombre(), inscripcion.getComision().getProfesor(),
                inscripcion.getFechaInscripcion());

        registrar(alumno, TipoCertificado.COMPROBANTE_CURSADA, pdf);
        return pdf;
    }

    private byte[] emitirComprobanteFinal(InscripcionFinal inscripcion) {
        Alumno alumno = inscripcion.getAlumno();
        Materia materia = inscripcion.getMesa().getMateria();
        String numero = "FIN-" + String.format("%06d", inscripcion.getId()) + "-" + inscripcion.getFechaInscripcion().getYear();

        MesaExamen mesa = inscripcion.getMesa();
        byte[] pdf = certificadoFactory.generarComprobanteFinal(alumno, numero, resolverCarrera(materia),
                materia.getNombre(), mesa.getFecha(), mesa.getHora(), mesa.getTurno().name(), mesa.getPeriodo(),
                mesa.getAula(), mesa.getModalidad() == null ? null : mesa.getModalidad().name(), mesa.getProfesor(),
                mesa.getEstado().name(), inscripcion.getEstado().name(), inscripcion.getFechaInscripcion());

        registrar(alumno, TipoCertificado.COMPROBANTE_FINAL, pdf);
        return pdf;
    }

    private String resolverCarrera(Materia materia) {
        return planMateriaRepository.buscarCarreraVigentePorMateria(materia.getId()).orElse(null);
    }

    /** Los comprobantes de inscripcion no tienen una URL fija: dependen del id de la inscripcion,
     * que no se guarda en CertificadoEmitido (solo alumno+tipo+fecha+hash, como todo lo demas). */
    private String urlPara(TipoCertificado tipo) {
        return switch (tipo) {
            case ALUMNO_REGULAR -> "/api/v1/certificados/alumno-regular";
            case ANALITICO_PARCIAL -> "/api/v1/certificados/analitico-parcial";
            case COMPROBANTE_CURSADA, COMPROBANTE_FINAL -> null;
        };
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
