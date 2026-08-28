package com.instituto.siga.service.impl;

import com.instituto.siga.dto.response.EventoCalendarioResponse;
import com.instituto.siga.entity.Comision;
import com.instituto.siga.entity.ConfiguracionInstitucional;
import com.instituto.siga.entity.EstadoInscripcion;
import com.instituto.siga.entity.EventoAcademico;
import com.instituto.siga.entity.Horario;
import com.instituto.siga.entity.InscripcionCursada;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.entity.TipoEventoCalendario;
import com.instituto.siga.entity.Turno;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.ComisionRepository;
import com.instituto.siga.repository.ConfiguracionInstitucionalRepository;
import com.instituto.siga.repository.EventoAcademicoRepository;
import com.instituto.siga.repository.InscripcionCursadaRepository;
import com.instituto.siga.repository.InscripcionFinalRepository;
import com.instituto.siga.repository.MesaExamenRepository;
import com.instituto.siga.service.interfaces.CalendarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Arma el calendario en tiempo real a partir de las tablas que ya existen (horario, mesa_examen,
 * inscripcion_cursada, inscripcion_final, configuracion_institucional) mas evento_academico para
 * lo que no tiene origen. No persiste ninguna copia: docs/04 seccion Calendario.
 */
@Service
@RequiredArgsConstructor
public class CalendarioServiceImpl implements CalendarioService {

    private static final List<EstadoInscripcion> ACTIVOS = List.of(EstadoInscripcion.INSCRIPTO, EstadoInscripcion.MODIFICADA);

    private final InscripcionCursadaRepository inscripcionCursadaRepository;
    private final InscripcionFinalRepository inscripcionFinalRepository;
    private final ComisionRepository comisionRepository;
    private final MesaExamenRepository mesaExamenRepository;
    private final ConfiguracionInstitucionalRepository configuracionRepository;
    private final EventoAcademicoRepository eventoAcademicoRepository;
    private final AlumnoRepository alumnoRepository;

    @Override
    @PreAuthorize("hasRole('ALUMNO')")
    @Transactional(readOnly = true)
    public List<EventoCalendarioResponse> calendarioPropio(Long alumnoId, LocalDate desde, LocalDate hasta) {
        return armarCalendarioDeAlumno(alumnoId, desde, hasta);
    }

    @Override
    @PreAuthorize("hasRole('PRECEPTOR') or hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public List<EventoCalendarioResponse> calendarioDeAlumno(Long alumnoId, LocalDate desde, LocalDate hasta) {
        if (!alumnoRepository.existsById(alumnoId)) {
            throw new ResourceNotFoundException("Alumno no encontrado: " + alumnoId);
        }
        return armarCalendarioDeAlumno(alumnoId, desde, hasta);
    }

    @Override
    @PreAuthorize("hasRole('PRECEPTOR') or hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public List<EventoCalendarioResponse> calendarioInstitucional(LocalDate desde, LocalDate hasta) {
        LocalDate[] rango = normalizarRango(desde, hasta);
        List<EventoCalendarioResponse> eventos = new ArrayList<>();

        eventos.addAll(expandirHorarios(comisionRepository.findAll(), rango[0], rango[1]));

        mesaExamenRepository.findAll().stream()
                .filter(m -> !m.getFecha().isBefore(rango[0]) && !m.getFecha().isAfter(rango[1]))
                .map(this::eventoDeMesa)
                .forEach(eventos::add);

        agregarEventosInstitucionales(eventos, rango);

        return ordenados(eventos);
    }

    private List<EventoCalendarioResponse> armarCalendarioDeAlumno(Long alumnoId, LocalDate desde, LocalDate hasta) {
        LocalDate[] rango = normalizarRango(desde, hasta);
        List<EventoCalendarioResponse> eventos = new ArrayList<>();

        List<InscripcionCursada> cursadas = inscripcionCursadaRepository.findByAlumnoIdAndEstadoIn(alumnoId, ACTIVOS);
        eventos.addAll(expandirHorarios(cursadas.stream().map(InscripcionCursada::getComision).toList(), rango[0], rango[1]));
        cursadas.stream()
                .filter(i -> dentroDeRango(i.getFechaInscripcion(), rango))
                .map(this::eventoDeInscripcionCursada)
                .forEach(eventos::add);

        List<InscripcionFinal> finales = inscripcionFinalRepository.findByAlumnoIdAndEstadoIn(alumnoId, ACTIVOS);
        finales.stream()
                .map(InscripcionFinal::getMesa)
                .filter(m -> !m.getFecha().isBefore(rango[0]) && !m.getFecha().isAfter(rango[1]))
                .map(this::eventoDeMesa)
                .forEach(eventos::add);
        finales.stream()
                .filter(i -> dentroDeRango(i.getFechaInscripcion(), rango))
                .map(this::eventoDeInscripcionFinal)
                .forEach(eventos::add);

        agregarEventosInstitucionales(eventos, rango);

        return ordenados(eventos);
    }

    private void agregarEventosInstitucionales(List<EventoCalendarioResponse> eventos, LocalDate[] rango) {
        configuracionRepository.findById(1).ifPresent(config ->
                eventosDeConfiguracion(config).stream()
                        .filter(e -> dentroDeRango(e.inicio(), rango))
                        .forEach(eventos::add));

        eventoAcademicoRepository.buscarEnRango(rango[0].atStartOfDay(), rango[1].atTime(LocalTime.MAX)).stream()
                .map(this::eventoDeEventoAcademico)
                .forEach(eventos::add);
    }

    private List<EventoCalendarioResponse> expandirHorarios(List<Comision> comisiones, LocalDate desde, LocalDate hasta) {
        List<EventoCalendarioResponse> eventos = new ArrayList<>();
        for (Comision comision : comisiones) {
            for (Horario horario : comision.getHorarios()) {
                for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
                    if (fecha.getDayOfWeek().getValue() == horario.getDiaSemana()) {
                        eventos.add(new EventoCalendarioResponse(
                                "horario-" + horario.getId() + "-" + fecha,
                                comision.getMateria().getNombre() + " (" + comision.getNombre() + ")",
                                detalleCursada(comision),
                                LocalDateTime.of(fecha, horario.getHoraInicio()),
                                LocalDateTime.of(fecha, horario.getHoraFin()),
                                TipoEventoCalendario.CURSADA,
                                horario.getAula()));
                    }
                }
            }
        }
        return eventos;
    }

    private String detalleCursada(Comision comision) {
        return comision.getProfesor() == null ? "Cursada" : "Cursada — Prof. " + comision.getProfesor();
    }

    private EventoCalendarioResponse eventoDeMesa(MesaExamen mesa) {
        LocalTime[] franja = franjaHoraria(mesa.getTurno());
        String detalle = "Turno " + mesa.getTurno().name()
                + (mesa.getProfesor() == null ? "" : " — Prof. " + mesa.getProfesor());
        return new EventoCalendarioResponse(
                "mesa-" + mesa.getId(),
                "Final: " + mesa.getMateria().getNombre(),
                detalle,
                LocalDateTime.of(mesa.getFecha(), franja[0]),
                LocalDateTime.of(mesa.getFecha(), franja[1]),
                TipoEventoCalendario.FINAL,
                null);
    }

    /** MesaExamen solo guarda fecha, sin hora: el turno se mapea a una franja nominal. */
    private LocalTime[] franjaHoraria(Turno turno) {
        return switch (turno) {
            case MANANA -> new LocalTime[] { LocalTime.of(9, 0), LocalTime.of(12, 0) };
            case TARDE -> new LocalTime[] { LocalTime.of(14, 0), LocalTime.of(17, 0) };
            case NOCHE -> new LocalTime[] { LocalTime.of(18, 0), LocalTime.of(21, 0) };
        };
    }

    private EventoCalendarioResponse eventoDeInscripcionCursada(InscripcionCursada inscripcion) {
        return new EventoCalendarioResponse(
                "insc-cursada-" + inscripcion.getId(),
                "Inscripción a cursada: " + inscripcion.getComision().getMateria().getNombre(),
                "Comisión " + inscripcion.getComision().getNombre(),
                inscripcion.getFechaInscripcion(),
                inscripcion.getFechaInscripcion(),
                TipoEventoCalendario.INSCRIPCION,
                null);
    }

    private EventoCalendarioResponse eventoDeInscripcionFinal(InscripcionFinal inscripcion) {
        return new EventoCalendarioResponse(
                "insc-final-" + inscripcion.getId(),
                "Inscripción a final: " + inscripcion.getMesa().getMateria().getNombre(),
                "Mesa del " + inscripcion.getMesa().getFecha(),
                inscripcion.getFechaInscripcion(),
                inscripcion.getFechaInscripcion(),
                TipoEventoCalendario.INSCRIPCION,
                null);
    }

    private List<EventoCalendarioResponse> eventosDeConfiguracion(ConfiguracionInstitucional config) {
        return List.of(
                new EventoCalendarioResponse("config-apertura-cursadas", "Apertura de inscripción a cursadas", null,
                        config.getFechaAperturaCursadas(), config.getFechaAperturaCursadas(),
                        TipoEventoCalendario.INSCRIPCION, null),
                new EventoCalendarioResponse("config-cierre-cursadas", "Cierre de inscripción a cursadas", null,
                        config.getFechaCierreCursadas(), config.getFechaCierreCursadas(),
                        TipoEventoCalendario.FECHA_LIMITE, null),
                new EventoCalendarioResponse("config-apertura-finales", "Apertura de inscripción a finales", null,
                        config.getFechaAperturaFinales(), config.getFechaAperturaFinales(),
                        TipoEventoCalendario.INSCRIPCION, null),
                new EventoCalendarioResponse("config-cierre-finales", "Cierre de inscripción a finales", null,
                        config.getFechaCierreFinales(), config.getFechaCierreFinales(),
                        TipoEventoCalendario.FECHA_LIMITE, null));
    }

    private EventoCalendarioResponse eventoDeEventoAcademico(EventoAcademico evento) {
        TipoEventoCalendario tipo = TipoEventoCalendario.valueOf(evento.getTipo().name());
        return new EventoCalendarioResponse(
                "evento-" + evento.getId(),
                evento.getTitulo(),
                evento.getDescripcion(),
                evento.getFechaInicio(),
                evento.getFechaFin(),
                tipo,
                null);
    }

    private boolean dentroDeRango(LocalDateTime fecha, LocalDate[] rango) {
        LocalDate dia = fecha.toLocalDate();
        return !dia.isBefore(rango[0]) && !dia.isAfter(rango[1]);
    }

    private LocalDate[] normalizarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null) {
            return new LocalDate[] { desde, hasta };
        }
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        return new LocalDate[] { inicioMes, inicioMes.plusMonths(1).minusDays(1) };
    }

    private List<EventoCalendarioResponse> ordenados(List<EventoCalendarioResponse> eventos) {
        return eventos.stream().sorted(Comparator.comparing(EventoCalendarioResponse::inicio)).toList();
    }
}
