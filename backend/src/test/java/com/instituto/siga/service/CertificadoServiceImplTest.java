package com.instituto.siga.service;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.certificado.CertificadoFactory;
import com.instituto.siga.dto.response.CertificadoResponse;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.CertificadoEmitido;
import com.instituto.siga.entity.TipoCertificado;
import com.instituto.siga.entity.Usuario;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.CertificadoEmitidoRepository;
import com.instituto.siga.repository.EstadoAcademicoMateriaRepository;
import com.instituto.siga.repository.InscripcionCursadaRepository;
import com.instituto.siga.repository.InscripcionFinalRepository;
import com.instituto.siga.repository.PlanMateriaRepository;
import com.instituto.siga.service.impl.CertificadoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificadoServiceImplTest {

    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private EstadoAcademicoMateriaRepository estadoAcademicoMateriaRepository;
    @Mock
    private CertificadoEmitidoRepository certificadoEmitidoRepository;
    @Mock
    private InscripcionCursadaRepository inscripcionCursadaRepository;
    @Mock
    private InscripcionFinalRepository inscripcionFinalRepository;
    @Mock
    private PlanMateriaRepository planMateriaRepository;
    @Mock
    private CertificadoFactory certificadoFactory;
    @Mock
    private AuditoriaPublisher auditoriaPublisher;

    private CertificadoServiceImpl certificadoService;

    @BeforeEach
    void setUp() {
        certificadoService = new CertificadoServiceImpl(alumnoRepository, estadoAcademicoMateriaRepository,
                certificadoEmitidoRepository, inscripcionCursadaRepository, inscripcionFinalRepository,
                planMateriaRepository, certificadoFactory, auditoriaPublisher);
    }

    private Alumno alumnoDeEjemplo() {
        Usuario usuario = Usuario.builder().id(1L).dni("30111111").nombre("Juan").apellido("Perez").build();
        return Alumno.builder().usuarioId(1L).usuario(usuario).legajo("LEG-0001").fechaIngreso(LocalDate.now()).build();
    }

    @Test
    void generarAlumnoRegularPropio_conAlumnoInexistente_lanzaResourceNotFoundException() {
        when(alumnoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificadoService.generarAlumnoRegularPropio(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generarAlumnoRegularPropio_persisteConHashYDevuelvePdf() {
        Alumno alumno = alumnoDeEjemplo();
        byte[] pdfFalso = "contenido-pdf".getBytes(StandardCharsets.UTF_8);
        when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(certificadoFactory.generarAlumnoRegular(alumno)).thenReturn(pdfFalso);
        when(certificadoEmitidoRepository.save(any(CertificadoEmitido.class))).thenAnswer(inv -> {
            CertificadoEmitido c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        byte[] resultado = certificadoService.generarAlumnoRegularPropio(1L);

        assertThat(resultado).isEqualTo(pdfFalso);
        ArgumentCaptor<CertificadoEmitido> captor = ArgumentCaptor.forClass(CertificadoEmitido.class);
        org.mockito.Mockito.verify(certificadoEmitidoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoCertificado.ALUMNO_REGULAR);
        assertThat(captor.getValue().getHashDocumento()).hasSize(64);
    }

    @Test
    void generarAnaliticoParcialPropio_persisteTipoCorrecto() {
        Alumno alumno = alumnoDeEjemplo();
        byte[] pdfFalso = "analitico".getBytes(StandardCharsets.UTF_8);
        when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(estadoAcademicoMateriaRepository.findByAlumnoId(1L)).thenReturn(List.of());
        when(certificadoFactory.generarAnaliticoParcial(any(), any())).thenReturn(pdfFalso);
        when(certificadoEmitidoRepository.save(any(CertificadoEmitido.class))).thenAnswer(inv -> inv.getArgument(0));

        byte[] resultado = certificadoService.generarAnaliticoParcialPropio(1L);

        assertThat(resultado).isEqualTo(pdfFalso);
        ArgumentCaptor<CertificadoEmitido> captor = ArgumentCaptor.forClass(CertificadoEmitido.class);
        org.mockito.Mockito.verify(certificadoEmitidoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoCertificado.ANALITICO_PARCIAL);
    }

    @Test
    void historialPropio_mapeaUrlDescargaSegunTipo() {
        Alumno alumno = alumnoDeEjemplo();
        CertificadoEmitido regular = CertificadoEmitido.builder()
                .id(1L).alumno(alumno).tipo(TipoCertificado.ALUMNO_REGULAR)
                .fechaEmision(LocalDateTime.now()).hashDocumento("abc").build();
        CertificadoEmitido analitico = CertificadoEmitido.builder()
                .id(2L).alumno(alumno).tipo(TipoCertificado.ANALITICO_PARCIAL)
                .fechaEmision(LocalDateTime.now()).hashDocumento("def").build();

        when(alumnoRepository.findById(1L)).thenReturn(Optional.of(alumno));
        when(certificadoEmitidoRepository.findByAlumnoId(1L)).thenReturn(List.of(regular, analitico));

        List<CertificadoResponse> resultado = certificadoService.historialPropio(1L);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).urlDescarga()).isEqualTo("/api/v1/certificados/alumno-regular");
        assertThat(resultado.get(1).urlDescarga()).isEqualTo("/api/v1/certificados/analitico-parcial");
    }
}
