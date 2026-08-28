package com.instituto.siga.service;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.Comision;
import com.instituto.siga.entity.EstadoInscripcion;
import com.instituto.siga.entity.InscripcionCursada;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.entity.Turno;
import com.instituto.siga.entity.Usuario;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.ComisionRepository;
import com.instituto.siga.repository.InscripcionCursadaRepository;
import com.instituto.siga.repository.InscripcionFinalRepository;
import com.instituto.siga.repository.MesaExamenRepository;
import com.instituto.siga.service.impl.ExportacionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportacionServiceImplTest {

    @Mock
    private ComisionRepository comisionRepository;
    @Mock
    private InscripcionCursadaRepository inscripcionCursadaRepository;
    @Mock
    private MesaExamenRepository mesaExamenRepository;
    @Mock
    private InscripcionFinalRepository inscripcionFinalRepository;
    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private AuditoriaPublisher auditoriaPublisher;

    private ExportacionServiceImpl exportacionService;

    @BeforeEach
    void setUp() {
        exportacionService = new ExportacionServiceImpl(comisionRepository, inscripcionCursadaRepository,
                mesaExamenRepository, inscripcionFinalRepository, alumnoRepository, auditoriaPublisher);
    }

    private Alumno alumnoDeEjemplo() {
        Usuario usuario = Usuario.builder().id(1L).dni("30111111").nombre("Juan").apellido("Perez").build();
        return Alumno.builder().usuarioId(1L).usuario(usuario).legajo("LEG-0001").fechaIngreso(LocalDate.now()).build();
    }

    private void assertXlsxValido(byte[] datos) {
        assertThat(datos).isNotEmpty();
        assertThat(datos[0]).isEqualTo((byte) 'P');
        assertThat(datos[1]).isEqualTo((byte) 'K');
    }

    @Test
    void exportarInscripcionesCursada_conComisionInexistente_lanzaResourceNotFoundException() {
        when(comisionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exportacionService.exportarInscripcionesCursada(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void exportarInscripcionesCursada_generaXlsxConLasInscripciones() {
        Materia materia = Materia.builder().id(1L).codigo("MAT1").nombre("Materia 1").build();
        Comision comision = Comision.builder().id(1L).materia(materia).nombre("A").periodoLectivo("2026-1").cupoMaximo(30).build();
        Alumno alumno = alumnoDeEjemplo();
        InscripcionCursada inscripcion = InscripcionCursada.builder()
                .id(1L).alumno(alumno).comision(comision).estado(EstadoInscripcion.INSCRIPTO)
                .fechaInscripcion(LocalDateTime.now()).build();

        when(comisionRepository.findById(1L)).thenReturn(Optional.of(comision));
        when(inscripcionCursadaRepository.findByComisionId(1L)).thenReturn(List.of(inscripcion));

        byte[] resultado = exportacionService.exportarInscripcionesCursada(1L);

        assertXlsxValido(resultado);
        org.mockito.Mockito.verify(auditoriaPublisher).publicar(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("EXPORTACION_EXCEL"),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void exportarInscripcionesFinal_conMesaInexistente_lanzaResourceNotFoundException() {
        when(mesaExamenRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exportacionService.exportarInscripcionesFinal(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void exportarInscripcionesFinal_generaXlsxConLasInscripciones() {
        Materia materia = Materia.builder().id(1L).codigo("MAT1").nombre("Materia 1").build();
        MesaExamen mesa = MesaExamen.builder().id(1L).materia(materia).fecha(LocalDate.now()).turno(Turno.MANANA).cupoMaximo(20).build();
        Alumno alumno = alumnoDeEjemplo();
        InscripcionFinal inscripcion = InscripcionFinal.builder()
                .id(1L).alumno(alumno).mesa(mesa).estado(EstadoInscripcion.INSCRIPTO)
                .fechaInscripcion(LocalDateTime.now()).build();

        when(mesaExamenRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(inscripcionFinalRepository.findByMesaId(1L)).thenReturn(List.of(inscripcion));

        byte[] resultado = exportacionService.exportarInscripcionesFinal(1L);

        assertXlsxValido(resultado);
    }

    @Test
    void exportarAlumnos_generaXlsxConLosResultadosDeLaBusqueda() {
        when(alumnoRepository.buscar("LEG-0001", null, null, null)).thenReturn(List.of(alumnoDeEjemplo()));

        byte[] resultado = exportacionService.exportarAlumnos("LEG-0001", null, null);

        assertXlsxValido(resultado);
    }

    @Test
    void exportarAlumnos_sinResultados_generaXlsxSoloConEncabezado() {
        when(alumnoRepository.buscar(null, null, null, null)).thenReturn(List.of());

        byte[] resultado = exportacionService.exportarAlumnos(null, null, null);

        assertXlsxValido(resultado);
    }
}
