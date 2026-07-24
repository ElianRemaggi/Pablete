package com.instituto.siga.service.impl;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.InscripcionFinalRequest;
import com.instituto.siga.dto.response.InscripcionFinalResponse;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.EstadoInscripcion;
import com.instituto.siga.entity.IntentoFinal;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.InscripcionFinalMapper;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.InscripcionFinalRepository;
import com.instituto.siga.repository.IntentoFinalRepository;
import com.instituto.siga.repository.MesaExamenRepository;
import com.instituto.siga.service.interfaces.InscripcionFinalService;
import com.instituto.siga.validation.ValidadorAntelacionMinima;
import com.instituto.siga.validation.ValidadorInscripcionFinal;
import com.instituto.siga.validation.ValidadorVentanaInscripcionFinal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Pipeline de validadores de docs/07-reglas-negocio-validaciones.md SS2. */
@Service
@RequiredArgsConstructor
public class InscripcionFinalServiceImpl implements InscripcionFinalService {

    private static final List<EstadoInscripcion> ACTIVOS = List.of(EstadoInscripcion.INSCRIPTO, EstadoInscripcion.MODIFICADA);

    private final InscripcionFinalRepository inscripcionFinalRepository;
    private final IntentoFinalRepository intentoFinalRepository;
    private final AlumnoRepository alumnoRepository;
    private final MesaExamenRepository mesaExamenRepository;
    private final InscripcionFinalMapper inscripcionFinalMapper;
    private final AuditoriaPublisher auditoriaPublisher;
    private final ValidadorAntelacionMinima validadorAntelacionMinima;
    private final List<ValidadorInscripcionFinal> validadores;

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional
    public InscripcionFinalResponse inscribir(Long alumnoId, InscripcionFinalRequest request) {
        Alumno alumno = buscarAlumnoOFallar(alumnoId);
        MesaExamen mesa = buscarMesaOFallar(request.mesaId());

        if (inscripcionFinalRepository.existsByAlumnoIdAndMesaIdAndEstadoIn(alumnoId, mesa.getId(), ACTIVOS)) {
            throw new ConflictException("INSCRIPCION_DUPLICADA", "Ya tiene una inscripción activa a esta mesa");
        }

        for (ValidadorInscripcionFinal validador : validadores) {
            validador.validar(alumno, mesa, null);
        }

        InscripcionFinal inscripcion = InscripcionFinal.builder()
                .alumno(alumno)
                .mesa(mesa)
                .estado(EstadoInscripcion.INSCRIPTO)
                .fechaInscripcion(LocalDateTime.now())
                .build();
        InscripcionFinal guardada = inscripcionFinalRepository.save(inscripcion);

        crearIntentoFinal(alumno, mesa, guardada);

        auditoriaPublisher.publicar(alumnoId, "INSCRIPCION_FINAL_ALTA", "InscripcionFinal", guardada.getId(),
                Map.of("mesaId", mesa.getId(), "materiaId", mesa.getMateria().getId()));

        return inscripcionFinalMapper.toResponse(guardada);
    }

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional
    public InscripcionFinalResponse modificar(Long alumnoId, Long inscripcionId, InscripcionFinalRequest request) {
        InscripcionFinal inscripcion = buscarPropiaOFallar(inscripcionId, alumnoId);
        // La mesa ACTUAL debe seguir teniendo margen de antelacion para poder tocarse (docs SS2).
        validadorAntelacionMinima.validar(inscripcion.getAlumno(), inscripcion.getMesa(), inscripcion);

        MesaExamen nuevaMesa = buscarMesaOFallar(request.mesaId());

        // Se re-corren todas las reglas salvo la ventana general de inscripcion (misma logica
        // que InscripcionCursadaServiceImpl.modificar): modificar no exige estar dentro de la
        // ventana general, solo respetar la antelacion minima de la nueva mesa elegida.
        for (ValidadorInscripcionFinal validador : validadores) {
            if (validador instanceof ValidadorVentanaInscripcionFinal) {
                continue;
            }
            validador.validar(inscripcion.getAlumno(), nuevaMesa, inscripcion);
        }

        inscripcion.setMesa(nuevaMesa);
        inscripcion.setEstado(EstadoInscripcion.MODIFICADA);
        inscripcion.setFechaModificacion(LocalDateTime.now());
        InscripcionFinal guardada = inscripcionFinalRepository.save(inscripcion);

        actualizarIntentoFinal(guardada, nuevaMesa);

        auditoriaPublisher.publicar(alumnoId, "INSCRIPCION_FINAL_MODIFICADA", "InscripcionFinal", guardada.getId(),
                Map.of("mesaId", nuevaMesa.getId(), "materiaId", nuevaMesa.getMateria().getId()));

        return inscripcionFinalMapper.toResponse(guardada);
    }

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional
    public void darDeBaja(Long alumnoId, Long inscripcionId) {
        InscripcionFinal inscripcion = buscarPropiaOFallar(inscripcionId, alumnoId);
        validadorAntelacionMinima.validar(inscripcion.getAlumno(), inscripcion.getMesa(), inscripcion);

        inscripcion.setEstado(EstadoInscripcion.BAJA);
        inscripcion.setFechaModificacion(LocalDateTime.now());
        InscripcionFinal guardada = inscripcionFinalRepository.save(inscripcion);

        // El intento no llego a rendirse: se libera el cupo de intentos.
        intentoFinalRepository.deleteAll(intentoFinalRepository.findByInscripcionFinalId(guardada.getId()));

        auditoriaPublisher.publicar(alumnoId, "INSCRIPCION_FINAL_BAJA", "InscripcionFinal", guardada.getId(), Map.of());
    }

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional(readOnly = true)
    public List<InscripcionFinalResponse> listarPropias(Long alumnoId) {
        return inscripcionFinalRepository.findByAlumnoIdAndEstadoIn(alumnoId, ACTIVOS).stream()
                .map(inscripcionFinalMapper::toResponse)
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('PRECEPTOR') or hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public List<InscripcionFinalResponse> listarPorMesa(Long mesaId) {
        return inscripcionFinalRepository.findByMesaId(mesaId).stream()
                .map(inscripcionFinalMapper::toResponse)
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('PRECEPTOR') or hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public List<InscripcionFinalResponse> listarPorAlumno(Long alumnoId) {
        return inscripcionFinalRepository.findByAlumnoId(alumnoId).stream()
                .map(inscripcionFinalMapper::toResponse)
                .toList();
    }

    private void crearIntentoFinal(Alumno alumno, MesaExamen mesa, InscripcionFinal inscripcion) {
        long intentosPrevios = intentoFinalRepository.contarActivosPorAlumnoYMateria(
                alumno.getUsuarioId(), mesa.getMateria().getId(), ACTIVOS);
        intentoFinalRepository.save(IntentoFinal.builder()
                .alumno(alumno)
                .materia(mesa.getMateria())
                .inscripcionFinal(inscripcion)
                .nroIntento((int) intentosPrevios + 1)
                .fecha(mesa.getFecha())
                .build());
    }

    private void actualizarIntentoFinal(InscripcionFinal inscripcion, MesaExamen nuevaMesa) {
        List<IntentoFinal> intentos = intentoFinalRepository.findByInscripcionFinalId(inscripcion.getId());
        for (IntentoFinal intento : intentos) {
            intento.setMateria(nuevaMesa.getMateria());
            intento.setFecha(nuevaMesa.getFecha());
        }
        intentoFinalRepository.saveAll(intentos);
    }

    private Alumno buscarAlumnoOFallar(Long id) {
        return alumnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alumno no encontrado: " + id));
    }

    private MesaExamen buscarMesaOFallar(Long id) {
        return mesaExamenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa de examen no encontrada: " + id));
    }

    private InscripcionFinal buscarPropiaOFallar(Long inscripcionId, Long alumnoId) {
        return inscripcionFinalRepository.findByIdAndAlumnoId(inscripcionId, alumnoId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción no encontrada: " + inscripcionId));
    }
}
