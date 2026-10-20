package com.instituto.siga.service;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.MesaExamenRequest;
import com.instituto.siga.dto.response.MesaExamenResponse;
import com.instituto.siga.entity.EstadoInscripcion;
import com.instituto.siga.entity.EstadoMesa;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.entity.Turno;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.mapper.MesaExamenMapper;
import com.instituto.siga.repository.InscripcionFinalRepository;
import com.instituto.siga.repository.IntentoFinalRepository;
import com.instituto.siga.repository.MateriaRepository;
import com.instituto.siga.repository.MesaExamenRepository;
import com.instituto.siga.service.impl.MesaExamenServiceImpl;
import com.instituto.siga.validation.VentanaInscripcionMesa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesaExamenServiceImplTest {

    @Mock
    private MesaExamenRepository mesaExamenRepository;
    @Mock
    private MateriaRepository materiaRepository;
    @Mock
    private MesaExamenMapper mesaExamenMapper;
    @Mock
    private AuditoriaPublisher auditoriaPublisher;
    @Mock
    private InscripcionFinalRepository inscripcionFinalRepository;
    @Mock
    private IntentoFinalRepository intentoFinalRepository;
    @Mock
    private VentanaInscripcionMesa ventanaInscripcionMesa;

    private MesaExamenServiceImpl mesaExamenService;
    private Materia materia;

    @BeforeEach
    void setUp() {
        mesaExamenService = new MesaExamenServiceImpl(mesaExamenRepository, materiaRepository, mesaExamenMapper,
                auditoriaPublisher, inscripcionFinalRepository, intentoFinalRepository, ventanaInscripcionMesa);
        materia = Materia.builder().id(1L).codigo("MAT101").nombre("Programación I").build();
        lenient().when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));
        lenient().when(mesaExamenRepository.save(any(MesaExamen.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(mesaExamenMapper.toResponse(any(MesaExamen.class))).thenAnswer(inv -> {
            MesaExamen m = inv.getArgument(0);
            return new MesaExamenResponse(m.getId(), 1L, "Programación I", m.getFecha(), "MANANA", m.getCupoMaximo(),
                    null, null, null, null, null, null, null, m.getEstado().name(), 0, 0, false);
        });
    }

    private MesaExamenRequest request(LocalDate fecha, LocalTime hora, LocalDateTime apertura, LocalDateTime cierre) {
        return new MesaExamenRequest(1L, fecha, Turno.MANANA, 30, null, hora, "2026-2", "Aula 1", null, apertura, cierre);
    }

    private MesaExamen mesa(EstadoMesa estado) {
        return MesaExamen.builder().id(7L).materia(materia).fecha(LocalDate.now().plusDays(30)).turno(Turno.MANANA)
                .cupoMaximo(30).estado(estado).build();
    }

    @Test
    void crear_conFechaPasada_lanzaBusinessException() {
        assertThatThrownBy(() -> mesaExamenService.crear(request(LocalDate.now().minusDays(1), null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("MESA_FUERA_DE_FECHA");
    }

    @Test
    void crear_conMesaYaExistente_lanzaConflictException() {
        LocalDate fecha = LocalDate.now().plusDays(30);
        when(mesaExamenRepository.existsByMateriaIdAndFechaAndTurno(1L, fecha, Turno.MANANA)).thenReturn(true);

        assertThatThrownBy(() -> mesaExamenService.crear(request(fecha, null, null, null)))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("MESA_DUPLICADA");
    }

    @Test
    void crear_conDatosValidos_nacePorDefectoEnBorrador() {
        LocalDate fecha = LocalDate.now().plusDays(30);

        MesaExamenResponse response = mesaExamenService.crear(request(fecha, null, null, null));

        assertThat(response.estado()).isEqualTo("BORRADOR");
    }

    @Test
    void crear_conSoloApertura_lanzaVentanaInvalida() {
        LocalDate fecha = LocalDate.now().plusDays(30);

        assertThatThrownBy(() -> mesaExamenService.crear(request(fecha, null, LocalDateTime.now(), null)))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("MESA_VENTANA_INVALIDA");
    }

    @Test
    void crear_conCierreAnteriorALaApertura_lanzaVentanaInvalida() {
        LocalDate fecha = LocalDate.now().plusDays(30);
        LocalDateTime apertura = LocalDateTime.now().plusDays(5);

        assertThatThrownBy(() -> mesaExamenService.crear(request(fecha, null, apertura, apertura.minusDays(1))))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("MESA_VENTANA_INVALIDA");
    }

    @Test
    void crear_conCierreDespuesDelExamen_lanzaVentanaInvalida() {
        LocalDate fecha = LocalDate.now().plusDays(30);
        LocalDateTime apertura = LocalDateTime.now().plusDays(1);
        LocalDateTime cierre = fecha.plusDays(1).atTime(8, 0);

        assertThatThrownBy(() -> mesaExamenService.crear(request(fecha, LocalTime.of(9, 0), apertura, cierre)))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("MESA_VENTANA_INVALIDA");
    }

    @Test
    void crear_conVentanaCoherente_persiste() {
        LocalDate fecha = LocalDate.now().plusDays(30);
        LocalDateTime apertura = LocalDateTime.now().plusDays(1);
        LocalDateTime cierre = fecha.minusDays(2).atTime(12, 0);

        MesaExamenResponse response = mesaExamenService.crear(request(fecha, LocalTime.of(9, 0), apertura, cierre));

        assertThat(response.estado()).isEqualTo("BORRADOR");
        verify(mesaExamenRepository).save(any(MesaExamen.class));
    }

    @Test
    void publicar_deBorrador_pasaAPublicada() {
        MesaExamen borrador = mesa(EstadoMesa.BORRADOR);
        when(mesaExamenRepository.findById(7L)).thenReturn(Optional.of(borrador));

        MesaExamenResponse response = mesaExamenService.publicar(7L);

        assertThat(response.estado()).isEqualTo("PUBLICADA");
    }

    @Test
    void publicar_deMesaCancelada_lanzaEstadoInvalido() {
        when(mesaExamenRepository.findById(7L)).thenReturn(Optional.of(mesa(EstadoMesa.CANCELADA)));

        assertThatThrownBy(() -> mesaExamenService.publicar(7L))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("MESA_ESTADO_INVALIDO");
    }

    @Test
    void publicar_conVentanaYaVencida_lanzaVentanaInvalida() {
        MesaExamen vencida = mesa(EstadoMesa.BORRADOR);
        vencida.setAperturaInscripcion(LocalDateTime.now().minusDays(10));
        vencida.setCierreInscripcion(LocalDateTime.now().minusDays(1));
        when(mesaExamenRepository.findById(7L)).thenReturn(Optional.of(vencida));
        when(ventanaInscripcionMesa.tieneVentanaPropia(vencida)).thenReturn(true);

        assertThatThrownBy(() -> mesaExamenService.publicar(7L))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("MESA_VENTANA_INVALIDA");
    }

    @Test
    void cerrar_soloAdmiteMesasPublicadas() {
        when(mesaExamenRepository.findById(7L)).thenReturn(Optional.of(mesa(EstadoMesa.BORRADOR)));

        assertThatThrownBy(() -> mesaExamenService.cerrar(7L))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("MESA_ESTADO_INVALIDO");
    }

    @Test
    void cancelar_daDeBajaLasInscripcionesActivasSinBorrarlas() {
        MesaExamen publicada = mesa(EstadoMesa.PUBLICADA);
        InscripcionFinal activa = InscripcionFinal.builder().id(1L).mesa(publicada).estado(EstadoInscripcion.INSCRIPTO).build();
        InscripcionFinal yaBaja = InscripcionFinal.builder().id(2L).mesa(publicada).estado(EstadoInscripcion.BAJA).build();
        when(mesaExamenRepository.findById(7L)).thenReturn(Optional.of(publicada));
        when(inscripcionFinalRepository.findByMesaId(7L)).thenReturn(List.of(activa, yaBaja));
        when(intentoFinalRepository.findByInscripcionFinalId(1L)).thenReturn(List.of());

        MesaExamenResponse response = mesaExamenService.cancelar(7L);

        assertThat(response.estado()).isEqualTo("CANCELADA");
        assertThat(activa.getEstado()).isEqualTo(EstadoInscripcion.BAJA);
        assertThat(activa.getFechaModificacion()).isNotNull();
        verify(inscripcionFinalRepository, never()).delete(any());
        verify(inscripcionFinalRepository, never()).deleteAll(any());
        verify(mesaExamenRepository, never()).deleteById(any());
    }

    @Test
    void cancelar_mesaYaCancelada_lanzaEstadoInvalido() {
        when(mesaExamenRepository.findById(7L)).thenReturn(Optional.of(mesa(EstadoMesa.CANCELADA)));

        assertThatThrownBy(() -> mesaExamenService.cancelar(7L))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("MESA_ESTADO_INVALIDO");
    }

    @Test
    void actualizar_mesaCerrada_lanzaEstadoInvalido() {
        when(mesaExamenRepository.findById(7L)).thenReturn(Optional.of(mesa(EstadoMesa.CERRADA)));

        assertThatThrownBy(() -> mesaExamenService.actualizar(7L, request(LocalDate.now().plusDays(30), null, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("MESA_ESTADO_INVALIDO");
    }

    @Test
    void actualizar_bajandoElCupoPorDebajoDeLosInscriptos_lanzaCupoInvalido() {
        when(mesaExamenRepository.findById(7L)).thenReturn(Optional.of(mesa(EstadoMesa.PUBLICADA)));
        when(inscripcionFinalRepository.countByMesaIdAndEstadoIn(any(), any())).thenReturn(10L);
        MesaExamenRequest conCupoChico = new MesaExamenRequest(1L, LocalDate.now().plusDays(30), Turno.MANANA, 5,
                null, null, null, null, null, null, null);

        assertThatThrownBy(() -> mesaExamenService.actualizar(7L, conCupoChico))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("CUPO_INVALIDO");
    }

    @Test
    void eliminar_mesaPublicada_lanzaConflictYNoBorra() {
        when(mesaExamenRepository.findById(7L)).thenReturn(Optional.of(mesa(EstadoMesa.PUBLICADA)));

        assertThatThrownBy(() -> mesaExamenService.eliminar(7L))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("MESA_CON_HISTORIAL");
        verify(mesaExamenRepository, never()).deleteById(any());
    }

    @Test
    void eliminar_borradorConInscripciones_lanzaConflict() {
        when(mesaExamenRepository.findById(7L)).thenReturn(Optional.of(mesa(EstadoMesa.BORRADOR)));
        when(inscripcionFinalRepository.existsByMesaId(7L)).thenReturn(true);

        assertThatThrownBy(() -> mesaExamenService.eliminar(7L))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void eliminar_borradorSinHistorial_borra() {
        when(mesaExamenRepository.findById(7L)).thenReturn(Optional.of(mesa(EstadoMesa.BORRADOR)));

        mesaExamenService.eliminar(7L);

        verify(mesaExamenRepository).deleteById(7L);
    }
}
