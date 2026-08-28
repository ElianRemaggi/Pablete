package com.instituto.siga.service.impl;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.Comision;
import com.instituto.siga.entity.InscripcionCursada;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.exportacion.ExcelReportBuilder;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.ComisionRepository;
import com.instituto.siga.repository.InscripcionCursadaRepository;
import com.instituto.siga.repository.InscripcionFinalRepository;
import com.instituto.siga.repository.MesaExamenRepository;
import com.instituto.siga.security.SecurityUtils;
import com.instituto.siga.service.interfaces.ExportacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExportacionServiceImpl implements ExportacionService {

    private final ComisionRepository comisionRepository;
    private final InscripcionCursadaRepository inscripcionCursadaRepository;
    private final MesaExamenRepository mesaExamenRepository;
    private final InscripcionFinalRepository inscripcionFinalRepository;
    private final AlumnoRepository alumnoRepository;
    private final AuditoriaPublisher auditoriaPublisher;

    @Override
    @PreAuthorize("hasAnyRole('PRECEPTOR', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public byte[] exportarInscripcionesCursada(Long comisionId) {
        Comision comision = comisionRepository.findById(comisionId)
                .orElseThrow(() -> new ResourceNotFoundException("Comision no encontrada: " + comisionId));

        ExcelReportBuilder reporte = new ExcelReportBuilder("Inscripciones Cursada")
                .encabezado("Legajo", "DNI", "Nombre", "Apellido", "Estado", "Fecha Inscripcion");
        for (InscripcionCursada inscripcion : inscripcionCursadaRepository.findByComisionId(comisionId)) {
            Alumno alumno = inscripcion.getAlumno();
            reporte.fila(alumno.getLegajo(), alumno.getUsuario().getDni(), alumno.getUsuario().getNombre(),
                    alumno.getUsuario().getApellido(), inscripcion.getEstado().name(),
                    inscripcion.getFechaInscripcion().toString());
        }

        byte[] excel = reporte.build();
        auditar("inscripciones-cursada", Map.of("comisionId", comisionId, "comisionNombre", comision.getNombre()));
        return excel;
    }

    @Override
    @PreAuthorize("hasAnyRole('PRECEPTOR', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public byte[] exportarInscripcionesFinal(Long mesaId) {
        MesaExamen mesa = mesaExamenRepository.findById(mesaId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa de examen no encontrada: " + mesaId));

        ExcelReportBuilder reporte = new ExcelReportBuilder("Inscripciones Final")
                .encabezado("Legajo", "DNI", "Nombre", "Apellido", "Estado", "Fecha Inscripcion");
        for (InscripcionFinal inscripcion : inscripcionFinalRepository.findByMesaId(mesaId)) {
            Alumno alumno = inscripcion.getAlumno();
            reporte.fila(alumno.getLegajo(), alumno.getUsuario().getDni(), alumno.getUsuario().getNombre(),
                    alumno.getUsuario().getApellido(), inscripcion.getEstado().name(),
                    inscripcion.getFechaInscripcion().toString());
        }

        byte[] excel = reporte.build();
        auditar("inscripciones-final", Map.of("mesaId", mesaId, "materiaId", mesa.getMateria().getId()));
        return excel;
    }

    @Override
    @PreAuthorize("hasAnyRole('PRECEPTOR', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public byte[] exportarAlumnos(String legajo, String dni, String apellido) {
        ExcelReportBuilder reporte = new ExcelReportBuilder("Alumnos")
                .encabezado("Legajo", "DNI", "Nombre", "Apellido", "Fecha Ingreso");
        for (Alumno alumno : alumnoRepository.buscar(legajo, dni, apellido, null)) {
            reporte.fila(alumno.getLegajo(), alumno.getUsuario().getDni(), alumno.getUsuario().getNombre(),
                    alumno.getUsuario().getApellido(), alumno.getFechaIngreso().toString());
        }

        byte[] excel = reporte.build();
        auditar("alumnos", Map.of("legajo", String.valueOf(legajo), "dni", String.valueOf(dni), "apellido", String.valueOf(apellido)));
        return excel;
    }

    private void auditar(String tipo, Map<String, Object> detalle) {
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "EXPORTACION_EXCEL", "Exportacion", null,
                Map.of("tipo", tipo, "filtros", detalle));
    }
}
