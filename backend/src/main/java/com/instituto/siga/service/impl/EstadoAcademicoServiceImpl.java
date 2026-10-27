package com.instituto.siga.service.impl;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.response.EstadoAcademicoResponse;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.EstadoAcademicoMateria;
import com.instituto.siga.entity.EstadoMateria;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.excel.HistorialExcelPath;
import com.instituto.siga.excel.HistorialExcelWriter;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.EstadoAcademicoMateriaRepository;
import com.instituto.siga.repository.MateriaRepository;
import com.instituto.siga.security.SecurityUtils;
import com.instituto.siga.service.interfaces.EstadoAcademicoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EstadoAcademicoServiceImpl implements EstadoAcademicoService {

    private final EstadoAcademicoMateriaRepository estadoRepository;
    private final AlumnoRepository alumnoRepository;
    private final MateriaRepository materiaRepository;
    private final AuditoriaPublisher auditoriaPublisher;
    private final HistorialExcelWriter historialExcelWriter;
    private final HistorialExcelPath historialExcelPath;

    @Override
    @Transactional(readOnly = true)
    public EstadoMateria obtenerEstado(Long alumnoId, Long materiaId) {
        return estadoRepository.findByAlumnoIdAndMateriaId(alumnoId, materiaId)
                .map(EstadoAcademicoMateria::getEstado)
                .orElse(EstadoMateria.PENDIENTE);
    }

    @Override
    @Transactional
    public void marcarCursando(Long alumnoId, Long materiaId) {
        EstadoAcademicoMateria estado = estadoRepository.findByAlumnoIdAndMateriaId(alumnoId, materiaId).orElse(null);

        if (estado == null) {
            Alumno alumno = alumnoRepository.findById(alumnoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado: " + alumnoId));
            Materia materia = materiaRepository.findById(materiaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + materiaId));
            estadoRepository.save(EstadoAcademicoMateria.builder()
                    .alumno(alumno).materia(materia)
                    .estado(EstadoMateria.CURSANDO).fechaActualizacion(LocalDateTime.now())
                    .build());
        } else if (estado.getEstado() == EstadoMateria.PENDIENTE) {
            estado.setEstado(EstadoMateria.CURSANDO);
            estado.setFechaActualizacion(LocalDateTime.now());
            estadoRepository.save(estado);
        }
        // Si ya esta CURSANDO/REGULAR/APROBADA/LIBRE no se pisa: inscribirse de nuevo no retrocede el estado.
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public EstadoAcademicoResponse fijarEstado(Long alumnoId, Long materiaId, EstadoMateria nuevoEstado, Integer nota) {
        Alumno alumno = alumnoRepository.findById(alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado: " + alumnoId));
        Materia materia = materiaRepository.findById(materiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + materiaId));

        EstadoAcademicoMateria estado = estadoRepository.findByAlumnoIdAndMateriaId(alumnoId, materiaId)
                .orElseGet(() -> EstadoAcademicoMateria.builder().alumno(alumno).materia(materia).build());
        estado.setEstado(nuevoEstado);
        estado.setNota(nota);
        estado.setFechaActualizacion(LocalDateTime.now());

        EstadoAcademicoMateria guardado = estadoRepository.save(estado);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "ESTADO_ACADEMICO_MODIFICADO",
                "EstadoAcademicoMateria", guardado.getId(),
                Map.of("alumnoId", alumnoId, "materiaId", materiaId, "estado", nuevoEstado.name()));

        HistorialExcelWriter.Resultado excel = sincronizarConExcel(alumno, materia, nuevoEstado, nota);

        return new EstadoAcademicoResponse(alumnoId, materiaId, materia.getNombre(), nuevoEstado.name(), nota,
                guardado.getFechaActualizacion(), excel.escrito(), excel.mensaje());
    }

    // Los 4 metodos siguientes los invocan servicios que ya verificaron el rol del personal; no tienen @PreAuthorize propio.

    @Override
    @Transactional
    public void acreditarCursada(Long alumnoId, Long materiaId) {
        EstadoAcademicoMateria estado = obtenerOCrear(alumnoId, materiaId);
        if (estado.getEstado() == null || estado.getEstado() == EstadoMateria.PENDIENTE
                || estado.getEstado() == EstadoMateria.CURSANDO || estado.getEstado() == EstadoMateria.LIBRE) {
            aplicar(estado, EstadoMateria.REGULAR, estado.getNota());
        }
    }

    @Override
    @Transactional
    public void revocarCursada(Long alumnoId, Long materiaId) {
        EstadoAcademicoMateria estado = obtenerOCrear(alumnoId, materiaId);
        if (estado.getEstado() == EstadoMateria.REGULAR) {
            aplicar(estado, EstadoMateria.CURSANDO, estado.getNota());
        }
    }

    @Override
    @Transactional
    public void acreditarFinal(Long alumnoId, Long materiaId, Integer nota) {
        aplicar(obtenerOCrear(alumnoId, materiaId), EstadoMateria.APROBADA, nota);
    }

    @Override
    @Transactional
    public void revocarFinal(Long alumnoId, Long materiaId) {
        EstadoAcademicoMateria estado = obtenerOCrear(alumnoId, materiaId);
        if (estado.getEstado() == EstadoMateria.APROBADA) {
            aplicar(estado, EstadoMateria.REGULAR, null);
        }
    }

    private EstadoAcademicoMateria obtenerOCrear(Long alumnoId, Long materiaId) {
        return estadoRepository.findByAlumnoIdAndMateriaId(alumnoId, materiaId).orElseGet(() -> {
            Alumno alumno = alumnoRepository.findById(alumnoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado: " + alumnoId));
            Materia materia = materiaRepository.findById(materiaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + materiaId));
            return EstadoAcademicoMateria.builder().alumno(alumno).materia(materia)
                    .estado(EstadoMateria.PENDIENTE).fechaActualizacion(LocalDateTime.now()).build();
        });
    }

    private void aplicar(EstadoAcademicoMateria estado, EstadoMateria nuevo, Integer nota) {
        estado.setEstado(nuevo);
        estado.setNota(nota);
        estado.setFechaActualizacion(LocalDateTime.now());
        estadoRepository.save(estado);
        sincronizarConExcel(estado.getAlumno(), estado.getMateria(), nuevo, nota);
    }

    /** Escritura hacia el .xlsx configurado (frontend -> API -> backend -> XLSX): si el archivo no tiene fila para
     * este alumno/materia, esta bloqueado o no existe, el writer lo informa y no rompe la actualizacion en base,
     * que ya se confirmo arriba. El resultado vuelve al cliente para que sepa si el Excel quedo al dia. La fecha
     * solo se escribe al aprobar: es la fecha del final, no la de cualquier cambio de estado. */
    private HistorialExcelWriter.Resultado sincronizarConExcel(Alumno alumno, Materia materia, EstadoMateria estado, Integer nota) {
        try {
            String dni = alumno.getUsuario().getDni();
            LocalDate fecha = estado == EstadoMateria.APROBADA ? LocalDate.now() : null;
            HistorialExcelWriter.Resultado resultado = historialExcelWriter.actualizarCelda(
                    historialExcelPath.resolver(), dni, materia.getNombre(), estado, nota, fecha);
            return resultado != null ? resultado
                    : new HistorialExcelWriter.Resultado(HistorialExcelWriter.Resultado.Estado.ERROR, "Sin respuesta del escritor de Excel");
        } catch (Exception e) {
            log.warn("No se pudo sincronizar el cambio de estado academico con el Excel", e);
            return new HistorialExcelWriter.Resultado(HistorialExcelWriter.Resultado.Estado.ERROR,
                    "No se pudo sincronizar con el Excel: " + e.getMessage());
        }
    }
}
