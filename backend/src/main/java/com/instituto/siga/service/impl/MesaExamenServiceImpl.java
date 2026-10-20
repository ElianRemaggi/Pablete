package com.instituto.siga.service.impl;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.MesaExamenRequest;
import com.instituto.siga.dto.response.MesaExamenResponse;
import com.instituto.siga.entity.EstadoInscripcion;
import com.instituto.siga.entity.EstadoMesa;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.MesaExamenMapper;
import com.instituto.siga.repository.InscripcionFinalRepository;
import com.instituto.siga.repository.IntentoFinalRepository;
import com.instituto.siga.repository.MateriaRepository;
import com.instituto.siga.repository.MesaExamenRepository;
import com.instituto.siga.security.SecurityUtils;
import com.instituto.siga.service.interfaces.MesaExamenService;
import com.instituto.siga.validation.VentanaInscripcionMesa;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Reglas de docs/07-reglas-negocio-validaciones.md SS4.4 + ciclo de vida de la primera entrega
 * (BORRADOR -> PUBLICADA -> CERRADA / CANCELADA). La validacion contra el "calendario academico vigente"
 * sigue pendiente (no hay una entidad que lo modele); por ahora se exige que la fecha no sea anterior a hoy.
 */
@Service
@RequiredArgsConstructor
public class MesaExamenServiceImpl implements MesaExamenService {

    private static final List<EstadoInscripcion> ACTIVOS = List.of(EstadoInscripcion.INSCRIPTO, EstadoInscripcion.MODIFICADA);

    private final MesaExamenRepository mesaExamenRepository;
    private final MateriaRepository materiaRepository;
    private final MesaExamenMapper mesaExamenMapper;
    private final AuditoriaPublisher auditoriaPublisher;
    private final InscripcionFinalRepository inscripcionFinalRepository;
    private final IntentoFinalRepository intentoFinalRepository;
    private final VentanaInscripcionMesa ventanaInscripcionMesa;

    @Override
    @Transactional(readOnly = true)
    public List<MesaExamenResponse> listar(Long materiaId, LocalDate fecha, EstadoMesa estado, String periodo) {
        List<MesaExamen> mesas;
        if (materiaId != null && fecha != null) {
            mesas = mesaExamenRepository.findByMateriaIdAndFecha(materiaId, fecha);
        } else if (materiaId != null) {
            mesas = mesaExamenRepository.findByMateriaId(materiaId);
        } else if (fecha != null) {
            mesas = mesaExamenRepository.findByFecha(fecha);
        } else {
            mesas = mesaExamenRepository.findAll();
        }
        boolean soloPublicadas = !veTodasLasMesas();
        return mesas.stream()
                .filter(m -> !soloPublicadas || m.getEstado() == EstadoMesa.PUBLICADA)
                .filter(m -> estado == null || m.getEstado() == estado)
                .filter(m -> periodo == null || periodo.isBlank() || periodo.equalsIgnoreCase(m.getPeriodo()))
                .map(this::aResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MesaExamenResponse obtener(Long id) {
        MesaExamen mesa = buscarOFallar(id);
        if (!veTodasLasMesas() && mesa.getEstado() != EstadoMesa.PUBLICADA) {
            throw new ResourceNotFoundException("Mesa de examen no encontrada: " + id);
        }
        return aResponse(mesa);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public MesaExamenResponse crear(MesaExamenRequest request) {
        Materia materia = buscarMateriaOFallar(request.materiaId());
        validarFecha(request.fecha());
        validarVentana(request);

        if (mesaExamenRepository.existsByMateriaIdAndFechaAndTurno(
                request.materiaId(), request.fecha(), request.turno())) {
            throw new ConflictException("MESA_DUPLICADA",
                    "Ya existe una mesa de esa materia en esa fecha y turno");
        }

        MesaExamen mesa = MesaExamen.builder()
                .materia(materia)
                .fecha(request.fecha())
                .turno(request.turno())
                .cupoMaximo(request.cupoMaximo())
                .profesor(request.profesor())
                .hora(request.hora())
                .periodo(blankANull(request.periodo()))
                .aula(blankANull(request.aula()))
                .modalidad(request.modalidad())
                .aperturaInscripcion(request.aperturaInscripcion())
                .cierreInscripcion(request.cierreInscripcion())
                .estado(EstadoMesa.BORRADOR)
                .build();

        MesaExamen guardada = mesaExamenRepository.save(mesa);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "MESA_EXAMEN_CREADA", "MesaExamen", guardada.getId(),
                Map.of("materiaId", materia.getId(), "fecha", request.fecha().toString(), "turno", request.turno().name()));

        return aResponse(guardada);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public MesaExamenResponse actualizar(Long id, MesaExamenRequest request) {
        MesaExamen mesa = buscarOFallar(id);
        if (mesa.getEstado() == EstadoMesa.CANCELADA || mesa.getEstado() == EstadoMesa.CERRADA) {
            throw new BusinessException("MESA_ESTADO_INVALIDO",
                    "No se puede modificar una mesa " + mesa.getEstado().name().toLowerCase()
                            + (mesa.getEstado() == EstadoMesa.CERRADA ? " (volvé a publicarla para editarla)" : ""));
        }
        Materia materia = buscarMateriaOFallar(request.materiaId());
        validarFecha(request.fecha());
        validarVentana(request);

        if (mesaExamenRepository.existsByMateriaIdAndFechaAndTurnoAndIdNot(
                request.materiaId(), request.fecha(), request.turno(), id)) {
            throw new ConflictException("MESA_DUPLICADA",
                    "Ya existe una mesa de esa materia en esa fecha y turno");
        }

        long inscriptos = inscripcionFinalRepository.countByMesaIdAndEstadoIn(id, ACTIVOS);
        if (inscriptos > 0) {
            if (!mesa.getMateria().getId().equals(materia.getId())) {
                throw new BusinessException("MESA_CON_INSCRIPCIONES",
                        "No se puede cambiar la materia de una mesa que ya tiene inscriptos");
            }
            if (request.cupoMaximo() < inscriptos) {
                throw new BusinessException("CUPO_INVALIDO",
                        "El cupo no puede ser menor a la cantidad de inscriptos actuales (" + inscriptos + ")");
            }
        }

        mesa.setMateria(materia);
        mesa.setFecha(request.fecha());
        mesa.setTurno(request.turno());
        mesa.setCupoMaximo(request.cupoMaximo());
        mesa.setProfesor(request.profesor());
        mesa.setHora(request.hora());
        mesa.setPeriodo(blankANull(request.periodo()));
        mesa.setAula(blankANull(request.aula()));
        mesa.setModalidad(request.modalidad());
        mesa.setAperturaInscripcion(request.aperturaInscripcion());
        mesa.setCierreInscripcion(request.cierreInscripcion());

        MesaExamen guardada = mesaExamenRepository.save(mesa);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "MESA_EXAMEN_ACTUALIZADA", "MesaExamen", guardada.getId(),
                Map.of("materiaId", materia.getId(), "fecha", request.fecha().toString(), "turno", request.turno().name()));

        return aResponse(guardada);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public void eliminar(Long id) {
        MesaExamen mesa = buscarOFallar(id);
        if (mesa.getEstado() != EstadoMesa.BORRADOR || inscripcionFinalRepository.existsByMesaId(id)) {
            throw new ConflictException("MESA_CON_HISTORIAL",
                    "Solo se pueden eliminar mesas en borrador sin inscripciones; cancelá la mesa para conservar su historial");
        }
        mesaExamenRepository.deleteById(id);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "MESA_EXAMEN_ELIMINADA", "MesaExamen", id, Map.of());
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public MesaExamenResponse publicar(Long id) {
        MesaExamen mesa = buscarOFallar(id);
        if (mesa.getEstado() != EstadoMesa.BORRADOR && mesa.getEstado() != EstadoMesa.CERRADA) {
            throw new BusinessException("MESA_ESTADO_INVALIDO",
                    "Solo se puede publicar una mesa en borrador o cerrada (estado actual: " + mesa.getEstado() + ")");
        }
        validarFecha(mesa.getFecha());
        if (ventanaInscripcionMesa.tieneVentanaPropia(mesa) && mesa.getCierreInscripcion().isBefore(LocalDateTime.now())) {
            throw new BusinessException("MESA_VENTANA_INVALIDA",
                    "El cierre de inscripción de la mesa ya pasó; ajustá la ventana antes de publicarla");
        }
        return cambiarEstado(mesa, EstadoMesa.PUBLICADA, "MESA_EXAMEN_PUBLICADA");
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public MesaExamenResponse cerrar(Long id) {
        MesaExamen mesa = buscarOFallar(id);
        if (mesa.getEstado() != EstadoMesa.PUBLICADA) {
            throw new BusinessException("MESA_ESTADO_INVALIDO",
                    "Solo se puede cerrar la inscripción de una mesa publicada (estado actual: " + mesa.getEstado() + ")");
        }
        return cambiarEstado(mesa, EstadoMesa.CERRADA, "MESA_EXAMEN_CERRADA");
    }

    /** Cancela la mesa sin borrar nada: las inscripciones activas pasan a BAJA (con su fecha de modificacion)
     * y se libera el intento de final que consumian, igual que en una baja voluntaria. */
    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public MesaExamenResponse cancelar(Long id) {
        MesaExamen mesa = buscarOFallar(id);
        if (mesa.getEstado() == EstadoMesa.CANCELADA) {
            throw new BusinessException("MESA_ESTADO_INVALIDO", "La mesa ya está cancelada");
        }
        LocalDateTime ahora = LocalDateTime.now();
        List<InscripcionFinal> activas = inscripcionFinalRepository.findByMesaId(id).stream()
                .filter(i -> ACTIVOS.contains(i.getEstado()))
                .toList();
        for (InscripcionFinal inscripcion : activas) {
            inscripcion.setEstado(EstadoInscripcion.BAJA);
            inscripcion.setFechaModificacion(ahora);
            intentoFinalRepository.deleteAll(intentoFinalRepository.findByInscripcionFinalId(inscripcion.getId()));
        }
        inscripcionFinalRepository.saveAll(activas);
        return cambiarEstado(mesa, EstadoMesa.CANCELADA, "MESA_EXAMEN_CANCELADA");
    }

    private MesaExamenResponse cambiarEstado(MesaExamen mesa, EstadoMesa nuevoEstado, String accion) {
        mesa.setEstado(nuevoEstado);
        MesaExamen guardada = mesaExamenRepository.save(mesa);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), accion, "MesaExamen", guardada.getId(),
                Map.of("estado", nuevoEstado.name()));
        return aResponse(guardada);
    }

    private MesaExamenResponse aResponse(MesaExamen mesa) {
        MesaExamenResponse base = mesaExamenMapper.toResponse(mesa);
        long inscriptos = inscripcionFinalRepository.countByMesaIdAndEstadoIn(mesa.getId(), ACTIVOS);
        boolean abierta = mesa.getEstado() == EstadoMesa.PUBLICADA
                && ventanaInscripcionMesa.estaAbierta(mesa, LocalDateTime.now());
        return new MesaExamenResponse(base.id(), base.materiaId(), base.materiaNombre(), base.fecha(), base.turno(),
                base.cupoMaximo(), base.profesor(), base.hora(), base.periodo(), base.aula(), base.modalidad(),
                base.aperturaInscripcion(), base.cierreInscripcion(), base.estado(), inscriptos,
                Math.max(0, mesa.getCupoMaximo() - inscriptos), abierta);
    }

    private boolean veTodasLasMesas() {
        return SecurityUtils.tieneAlgunRol("ADMINISTRADOR", "PRECEPTOR");
    }

    private MesaExamen buscarOFallar(Long id) {
        return mesaExamenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa de examen no encontrada: " + id));
    }

    private Materia buscarMateriaOFallar(Long id) {
        return materiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Materia no encontrada: " + id));
    }

    private void validarFecha(LocalDate fecha) {
        if (fecha.isBefore(LocalDate.now())) {
            throw new BusinessException("MESA_FUERA_DE_FECHA", "La fecha de la mesa no puede ser anterior a hoy");
        }
    }

    /** La ventana propia es todo-o-nada, ordenada, y debe cerrar antes de que termine el dia del examen. */
    private void validarVentana(MesaExamenRequest request) {
        LocalDateTime apertura = request.aperturaInscripcion();
        LocalDateTime cierre = request.cierreInscripcion();
        if (apertura == null && cierre == null) {
            return;
        }
        if (apertura == null || cierre == null) {
            throw new BusinessException("MESA_VENTANA_INVALIDA",
                    "Indicá apertura y cierre de inscripción juntos, o ninguno para usar el período general");
        }
        if (!cierre.isAfter(apertura)) {
            throw new BusinessException("MESA_VENTANA_INVALIDA",
                    "El cierre de inscripción debe ser posterior a la apertura");
        }
        LocalDateTime inicioExamen = request.hora() != null
                ? request.fecha().atTime(request.hora())
                : request.fecha().atTime(LocalTime.MAX);
        if (cierre.isAfter(inicioExamen)) {
            throw new BusinessException("MESA_VENTANA_INVALIDA",
                    "La inscripción no puede cerrar después del inicio del examen");
        }
    }

    private String blankANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
