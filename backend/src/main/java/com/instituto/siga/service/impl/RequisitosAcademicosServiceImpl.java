package com.instituto.siga.service.impl;

import com.instituto.siga.dto.response.HabilitacionFinalResponse;
import com.instituto.siga.dto.response.RequisitoResponse;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.Correlatividad;
import com.instituto.siga.entity.EstadoMateria;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.TipoRequisito;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.repository.CorrelatividadRepository;
import com.instituto.siga.repository.MateriaRepository;
import com.instituto.siga.service.interfaces.EstadoAcademicoService;
import com.instituto.siga.service.interfaces.RequisitosAcademicosService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequisitosAcademicosServiceImpl implements RequisitosAcademicosService {

    private final CorrelatividadRepository correlatividadRepository;
    private final EstadoAcademicoService estadoAcademicoService;
    private final MateriaRepository materiaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RequisitoResponse> evaluarCorrelativas(Long alumnoId, Materia materia, boolean paraFinal) {
        List<RequisitoResponse> requisitos = new ArrayList<>();
        for (Correlatividad c : correlatividadRepository.findByMateriaId(materia.getId())) {
            EstadoMateria estado = estadoAcademicoService.obtenerEstado(alumnoId, c.getMateriaRequerida().getId());
            boolean cumple = paraFinal || c.getTipoRequisito() == TipoRequisito.APROBAR
                    ? estado == EstadoMateria.APROBADA
                    : estado == EstadoMateria.REGULAR || estado == EstadoMateria.APROBADA;
            String nombre = c.getMateriaRequerida().getNombre();
            String descripcion = (paraFinal || c.getTipoRequisito() == TipoRequisito.APROBAR
                    ? "Final aprobado de " : "Cursada aprobada de ") + nombre;
            requisitos.add(new RequisitoResponse(c.getMateriaRequerida().getId(), nombre,
                    c.getTipoRequisito().name(), cumple, descripcion));
        }
        return requisitos;
    }

    @Override
    @Transactional(readOnly = true)
    public void validarParaCursada(Alumno alumno, Materia materia) {
        List<RequisitoResponse> pendientes = evaluarCorrelativas(alumno.getUsuarioId(), materia, false).stream()
                .filter(r -> !r.cumplido()).toList();
        if (!pendientes.isEmpty()) {
            throw new BusinessException("REGLA_NEGOCIO_CORRELATIVIDAD",
                    "No cumple la correlatividad requerida para inscribirse a esta materia.",
                    pendientes.stream().map(r -> "Materia requerida: " + r.materiaRequeridaNombre()
                            + " (" + r.tipoRequisito() + ")").toList());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validarParaFinal(Alumno alumno, Materia materia) {
        List<String> pendientes = pendientesParaFinal(alumno.getUsuarioId(), materia);
        if (!pendientes.isEmpty()) {
            boolean falta = !cursadaPropiaAprobada(alumno.getUsuarioId(), materia);
            throw new BusinessException(falta ? "MATERIA_NO_REGULARIZADA" : "REGLA_NEGOCIO_CORRELATIVIDAD",
                    "No podés inscribirte al final de " + materia.getNombre() + " porque " + pendientes.get(0),
                    pendientes);
        }
    }

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional(readOnly = true)
    public HabilitacionFinalResponse habilitacionFinalPropia(Long alumnoId, Long materiaId) {
        return habilitacion(alumnoId, materiaId);
    }

    @Override
    @PreAuthorize("hasAnyRole('PRECEPTOR', 'ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public HabilitacionFinalResponse habilitacionFinalDeAlumno(Long alumnoId, Long materiaId) {
        return habilitacion(alumnoId, materiaId);
    }

    private HabilitacionFinalResponse habilitacion(Long alumnoId, Long materiaId) {
        Materia materia = materiaRepository.findById(materiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + materiaId));
        List<String> pendientes = pendientesParaFinal(alumnoId, materia);
        return new HabilitacionFinalResponse(materia.getId(), materia.getNombre(),
                cursadaPropiaAprobada(alumnoId, materia), pendientes.isEmpty(),
                evaluarCorrelativas(alumnoId, materia, true), pendientes);
    }

    private boolean cursadaPropiaAprobada(Long alumnoId, Materia materia) {
        EstadoMateria estado = estadoAcademicoService.obtenerEstado(alumnoId, materia.getId());
        return estado == EstadoMateria.REGULAR || estado == EstadoMateria.APROBADA;
    }

    /** Frases que completan "No podés inscribirte al final de X porque ..." (la primera es la que se muestra). */
    private List<String> pendientesParaFinal(Long alumnoId, Materia materia) {
        List<String> pendientes = new ArrayList<>();
        if (!cursadaPropiaAprobada(alumnoId, materia)) {
            pendientes.add("todavía no tenés aprobada la cursada");
        }
        for (RequisitoResponse r : evaluarCorrelativas(alumnoId, materia, true)) {
            if (!r.cumplido()) {
                pendientes.add("todavía no tenés aprobado el final de " + r.materiaRequeridaNombre());
            }
        }
        return pendientes;
    }
}
