package com.instituto.siga.service.impl;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.CalificacionCursadaRequest;
import com.instituto.siga.dto.request.InscripcionCursadaRequest;
import com.instituto.siga.dto.response.InscripcionCursadaResponse;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.Comision;
import com.instituto.siga.entity.ConfiguracionInstitucional;
import com.instituto.siga.entity.EstadoInscripcion;
import com.instituto.siga.entity.InscripcionCursada;
import com.instituto.siga.entity.ResultadoCursada;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.InscripcionCursadaMapper;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.ComisionRepository;
import com.instituto.siga.repository.ConfiguracionInstitucionalRepository;
import com.instituto.siga.repository.InscripcionCursadaRepository;
import com.instituto.siga.repository.PlanMateriaRepository;
import com.instituto.siga.security.SecurityUtils;
import com.instituto.siga.service.interfaces.EstadoAcademicoService;
import com.instituto.siga.service.interfaces.InscripcionCursadaService;
import com.instituto.siga.validation.ValidadorInscripcionCursada;
import com.instituto.siga.validation.ValidadorVentanaInscripcion;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pipeline de validadores de docs/07-reglas-negocio-validaciones.md SS1. */
@Service
@RequiredArgsConstructor
public class InscripcionCursadaServiceImpl implements InscripcionCursadaService {

    private static final List<EstadoInscripcion> ACTIVOS = List.of(EstadoInscripcion.INSCRIPTO, EstadoInscripcion.MODIFICADA);

    private final InscripcionCursadaRepository inscripcionCursadaRepository;
    private final AlumnoRepository alumnoRepository;
    private final ComisionRepository comisionRepository;
    private final ConfiguracionInstitucionalRepository configuracionRepository;
    private final EstadoAcademicoService estadoAcademicoService;
    private final PlanMateriaRepository planMateriaRepository;
    private final InscripcionCursadaMapper inscripcionCursadaMapper;
    private final AuditoriaPublisher auditoriaPublisher;
    private final List<ValidadorInscripcionCursada> validadores; // orden por @Order de cada @Component

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional
    public InscripcionCursadaResponse inscribir(Long alumnoId, InscripcionCursadaRequest request) {
        Alumno alumno = buscarAlumnoOFallar(alumnoId);
        Comision comision = buscarComisionOFallar(request.comisionId());

        if (inscripcionCursadaRepository.existsByAlumnoIdAndComisionIdAndEstadoIn(alumnoId, comision.getId(), ACTIVOS)) {
            throw new ConflictException("INSCRIPCION_DUPLICADA", "Ya tiene una inscripción activa a esta comisión");
        }

        for (ValidadorInscripcionCursada validador : validadores) {
            validador.validar(alumno, comision, null);
        }

        InscripcionCursada inscripcion = InscripcionCursada.builder()
                .alumno(alumno)
                .comision(comision)
                .estado(EstadoInscripcion.INSCRIPTO)
                .fechaInscripcion(LocalDateTime.now())
                .build();

        InscripcionCursada guardada = inscripcionCursadaRepository.save(inscripcion);
        estadoAcademicoService.marcarCursando(alumnoId, comision.getMateria().getId());

        auditoriaPublisher.publicar(alumnoId, "INSCRIPCION_CURSADA_ALTA", "InscripcionCursada", guardada.getId(),
                Map.of("comisionId", comision.getId(), "materiaId", comision.getMateria().getId()));

        return enriquecer(guardada);
    }

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional
    public InscripcionCursadaResponse modificar(Long alumnoId, Long inscripcionId, InscripcionCursadaRequest request) {
        InscripcionCursada inscripcion = buscarPropiaOFallar(inscripcionId, alumnoId);
        Comision nuevaComision = buscarComisionOFallar(request.comisionId());

        // docs/07 SS1: modificar re-corre solo las reglas 1-3 (correlatividad, superposicion, cupo);
        // la ventana de modificacion es una regla propia (solo exige no haber pasado el cierre,
        // a diferencia del alta que exige estar dentro de todo el rango apertura-cierre).
        validarDentroDePlazoDeCierre();

        for (ValidadorInscripcionCursada validador : validadores) {
            if (validador instanceof ValidadorVentanaInscripcion) {
                continue;
            }
            validador.validar(inscripcion.getAlumno(), nuevaComision, inscripcion);
        }

        inscripcion.setComision(nuevaComision);
        inscripcion.setEstado(EstadoInscripcion.MODIFICADA);
        inscripcion.setFechaModificacion(LocalDateTime.now());

        InscripcionCursada guardada = inscripcionCursadaRepository.save(inscripcion);
        estadoAcademicoService.marcarCursando(alumnoId, nuevaComision.getMateria().getId());

        auditoriaPublisher.publicar(alumnoId, "INSCRIPCION_CURSADA_MODIFICADA", "InscripcionCursada", guardada.getId(),
                Map.of("comisionId", nuevaComision.getId(), "materiaId", nuevaComision.getMateria().getId()));

        return enriquecer(guardada);
    }

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional
    public void darDeBaja(Long alumnoId, Long inscripcionId) {
        InscripcionCursada inscripcion = buscarPropiaOFallar(inscripcionId, alumnoId);
        validarDentroDePlazoDeCierre();

        inscripcion.setEstado(EstadoInscripcion.BAJA);
        inscripcion.setFechaModificacion(LocalDateTime.now());
        InscripcionCursada guardada = inscripcionCursadaRepository.save(inscripcion);

        auditoriaPublisher.publicar(alumnoId, "INSCRIPCION_CURSADA_BAJA", "InscripcionCursada", guardada.getId(), Map.of());
    }

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional(readOnly = true)
    public List<InscripcionCursadaResponse> listarPropias(Long alumnoId) {
        return inscripcionCursadaRepository.findByAlumnoIdAndEstadoIn(alumnoId, ACTIVOS).stream()
                .map(this::enriquecer)
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('PRECEPTOR') or hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public List<InscripcionCursadaResponse> listarPorComision(Long comisionId) {
        return inscripcionCursadaRepository.findByComisionId(comisionId).stream()
                .map(this::enriquecer)
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('PRECEPTOR') or hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public List<InscripcionCursadaResponse> listarPorAlumno(Long alumnoId) {
        return inscripcionCursadaRepository.findByAlumnoId(alumnoId).stream()
                .map(this::enriquecer)
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('PRECEPTOR') or hasRole('ADMINISTRADOR')")
    @Transactional
    public InscripcionCursadaResponse calificar(Long inscripcionId, CalificacionCursadaRequest request) {
        InscripcionCursada inscripcion = inscripcionCursadaRepository.findById(inscripcionId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada: " + inscripcionId));
        if (inscripcion.getEstado() == EstadoInscripcion.BAJA) {
            throw new BusinessException("INSCRIPCION_DADA_DE_BAJA",
                    "No se puede calificar una inscripción dada de baja");
        }
        ResultadoCursada resultado = request.resultado();
        if (resultado == ResultadoCursada.PENDIENTE && request.nota() != null) {
            throw new BusinessException("NOTA_INVALIDA", "Una cursada pendiente no lleva nota");
        }
        if (resultado != ResultadoCursada.PENDIENTE && request.nota() == null) {
            throw new BusinessException("NOTA_INVALIDA",
                    "Para dejar la cursada " + resultado.name().toLowerCase() + " hay que cargar una nota válida (1 a 10)");
        }

        ResultadoCursada anterior = inscripcion.getResultadoCursada() == null
                ? ResultadoCursada.PENDIENTE : inscripcion.getResultadoCursada();
        Integer notaAnterior = inscripcion.getNotaCursada();
        Long usuarioId = SecurityUtils.usuarioActualId();

        inscripcion.setResultadoCursada(resultado);
        inscripcion.setNotaCursada(request.nota());
        inscripcion.setFechaCalificacion(LocalDateTime.now());
        inscripcion.setCalificadoPor(usuarioId);
        InscripcionCursada guardada = inscripcionCursadaRepository.save(inscripcion);

        Long alumnoId = guardada.getAlumno().getUsuarioId();
        Long materiaId = guardada.getComision().getMateria().getId();
        if (resultado == ResultadoCursada.APROBADA) {
            estadoAcademicoService.acreditarCursada(alumnoId, materiaId);
        } else if (anterior == ResultadoCursada.APROBADA
                && !inscripcionCursadaRepository.existeOtraCursadaAprobada(alumnoId, materiaId, guardada.getId())) {
            // La cursada deja de estar aprobada: se retira la regularidad, sin tocar un final ya aprobado.
            estadoAcademicoService.revocarCursada(alumnoId, materiaId);
        }

        Map<String, Object> detalle = new HashMap<>();
        detalle.put("alumnoId", alumnoId);
        detalle.put("materiaId", materiaId);
        detalle.put("comisionId", guardada.getComision().getId());
        detalle.put("resultadoAntes", anterior.name());
        detalle.put("resultadoDespues", resultado.name());
        detalle.put("notaAntes", notaAnterior);
        detalle.put("notaDespues", request.nota());
        auditoriaPublisher.publicar(usuarioId, "CURSADA_CALIFICADA", "InscripcionCursada", guardada.getId(), detalle);

        return enriquecer(guardada);
    }

    private Alumno buscarAlumnoOFallar(Long id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado: " + id));
    }

    private Comision buscarComisionOFallar(Long id) {
        return comisionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comisión no encontrada: " + id));
    }

    /** No revela si la inscripcion existe pero es de otro alumno: 404 en ambos casos. */
    private InscripcionCursada buscarPropiaOFallar(Long inscripcionId, Long alumnoId) {
        return inscripcionCursadaRepository.findByIdAndAlumnoId(inscripcionId, alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada: " + inscripcionId));
    }

    /** docs/07 SS1: "Modificación/baja permitida únicamente si now() <= fechaCierreCursadas". */
    private void validarDentroDePlazoDeCierre() {
        ConfiguracionInstitucional config = configuracionRepository.findById(1)
                .orElseThrow(() -> new ResourceNotFoundException("Configuración institucional no inicializada"));
        if (LocalDateTime.now().isAfter(config.getFechaCierreCursadas())) {
            throw new BusinessException("FUERA_DE_PLAZO", "El período de inscripción a cursadas ya cerró");
        }
    }

    /** El mapper deja carreraNombre en null (no es un campo de la entidad): se resuelve aca. */
    private InscripcionCursadaResponse enriquecer(InscripcionCursada entity) {
        InscripcionCursadaResponse base = inscripcionCursadaMapper.toResponse(entity);
        String carrera = planMateriaRepository.buscarCarreraVigentePorMateria(entity.getComision().getMateria().getId())
                .orElse(null);
        return new InscripcionCursadaResponse(base.id(), base.materiaNombre(), base.comisionNombre(), base.estado(),
                base.fechaInscripcion(), base.profesor(), carrera, base.horarios(), base.materiaId(), base.comisionId(),
                base.periodo(), base.alumnoId(), base.alumnoDni(), base.alumnoNombre(), base.alumnoApellido(),
                base.alumnoLegajo(), base.notaCursada(), base.resultadoCursada(), base.fechaCalificacion());
    }
}
