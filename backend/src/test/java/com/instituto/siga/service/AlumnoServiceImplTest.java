package com.instituto.siga.service;

import com.instituto.siga.dto.response.AlumnoResponse;
import com.instituto.siga.dto.response.HistorialAcademicoResponse;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.EstadoAcademicoMateria;
import com.instituto.siga.entity.EstadoMateria;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.Usuario;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.AlumnoMapper;
import com.instituto.siga.mapper.HistorialAcademicoMapper;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.EstadoAcademicoMateriaRepository;
import com.instituto.siga.service.impl.AlumnoServiceImpl;
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
class AlumnoServiceImplTest {

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private EstadoAcademicoMateriaRepository estadoAcademicoMateriaRepository;
    @Mock
    private AlumnoMapper alumnoMapper;
    @Mock
    private HistorialAcademicoMapper historialAcademicoMapper;

    private AlumnoServiceImpl alumnoService;

    @BeforeEach
    void setUp() {
        alumnoService = new AlumnoServiceImpl(alumnoRepository, estadoAcademicoMateriaRepository,
                alumnoMapper, historialAcademicoMapper);
    }

    private Alumno alumnoDeEjemplo() {
        Usuario usuario = Usuario.builder().id(1L).dni("30111111").nombre("Juan").apellido("Perez").build();
        return Alumno.builder().usuarioId(1L).usuario(usuario).legajo("LEG-0001").fechaIngreso(LocalDate.now()).build();
    }

    @Test
    void obtenerPropio_conIdInexistente_lanzaResourceNotFoundException() {
        when(alumnoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alumnoService.obtenerPropio(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerPropio_conIdExistente_devuelveResponse() {
        Alumno alumno = alumnoDeEjemplo();
        when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(alumnoMapper.toResponse(alumno))
                .thenReturn(new AlumnoResponse(1L, "30111111", "Juan", "Perez", "LEG-0001", LocalDate.now()));

        AlumnoResponse response = alumnoService.obtenerPropio(1L);

        assertThat(response.legajo()).isEqualTo("LEG-0001");
    }

    @Test
    void buscar_delegaEnRepositorioYMapeaCadaResultado() {
        Alumno alumno = alumnoDeEjemplo();
        when(alumnoRepository.buscar("LEG-0001", null, null)).thenReturn(List.of(alumno));
        when(alumnoMapper.toResponse(alumno))
                .thenReturn(new AlumnoResponse(1L, "30111111", "Juan", "Perez", "LEG-0001", LocalDate.now()));

        List<AlumnoResponse> resultado = alumnoService.buscar("LEG-0001", null, null);

        assertThat(resultado).hasSize(1);
    }

    @Test
    void materiasAprobadasPropias_filtraSoloAprobadas() {
        Alumno alumno = alumnoDeEjemplo();
        Materia materia1 = Materia.builder().id(1L).codigo("MAT1").nombre("Materia 1").build();
        Materia materia2 = Materia.builder().id(2L).codigo("MAT2").nombre("Materia 2").build();
        EstadoAcademicoMateria aprobada = EstadoAcademicoMateria.builder()
                .id(1L).alumno(alumno).materia(materia1).estado(EstadoMateria.APROBADA)
                .fechaActualizacion(LocalDateTime.now()).build();
        EstadoAcademicoMateria cursando = EstadoAcademicoMateria.builder()
                .id(2L).alumno(alumno).materia(materia2).estado(EstadoMateria.CURSANDO)
                .fechaActualizacion(LocalDateTime.now()).build();

        when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(estadoAcademicoMateriaRepository.findByAlumnoId(1L)).thenReturn(List.of(aprobada, cursando));
        when(historialAcademicoMapper.toResponse(aprobada))
                .thenReturn(new HistorialAcademicoResponse("MAT1", "Materia 1", "APROBADA", LocalDateTime.now()));
        when(historialAcademicoMapper.toResponse(cursando))
                .thenReturn(new HistorialAcademicoResponse("MAT2", "Materia 2", "CURSANDO", LocalDateTime.now()));

        List<HistorialAcademicoResponse> resultado = alumnoService.materiasAprobadasPropias(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).materiaCodigo()).isEqualTo("MAT1");
    }

    @Test
    void materiasPendientesPropias_excluyeAprobadas() {
        Alumno alumno = alumnoDeEjemplo();
        Materia materia1 = Materia.builder().id(1L).codigo("MAT1").nombre("Materia 1").build();
        Materia materia2 = Materia.builder().id(2L).codigo("MAT2").nombre("Materia 2").build();
        EstadoAcademicoMateria aprobada = EstadoAcademicoMateria.builder()
                .id(1L).alumno(alumno).materia(materia1).estado(EstadoMateria.APROBADA)
                .fechaActualizacion(LocalDateTime.now()).build();
        EstadoAcademicoMateria cursando = EstadoAcademicoMateria.builder()
                .id(2L).alumno(alumno).materia(materia2).estado(EstadoMateria.CURSANDO)
                .fechaActualizacion(LocalDateTime.now()).build();

        when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(estadoAcademicoMateriaRepository.findByAlumnoId(1L)).thenReturn(List.of(aprobada, cursando));
        when(historialAcademicoMapper.toResponse(aprobada))
                .thenReturn(new HistorialAcademicoResponse("MAT1", "Materia 1", "APROBADA", LocalDateTime.now()));
        when(historialAcademicoMapper.toResponse(cursando))
                .thenReturn(new HistorialAcademicoResponse("MAT2", "Materia 2", "CURSANDO", LocalDateTime.now()));

        List<HistorialAcademicoResponse> resultado = alumnoService.materiasPendientesPropias(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).materiaCodigo()).isEqualTo("MAT2");
    }
}
