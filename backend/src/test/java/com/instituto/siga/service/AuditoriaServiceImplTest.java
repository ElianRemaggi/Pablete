package com.instituto.siga.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.instituto.siga.audit.AuditoriaEvent;
import com.instituto.siga.dto.response.AuditoriaResponse;
import com.instituto.siga.entity.Auditoria;
import com.instituto.siga.entity.Usuario;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.AuditoriaMapper;
import com.instituto.siga.repository.AuditoriaRepository;
import com.instituto.siga.repository.UsuarioRepository;
import com.instituto.siga.service.impl.AuditoriaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceImplTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditoriaMapper auditoriaMapper;

    private AuditoriaServiceImpl auditoriaService;

    @BeforeEach
    void setUp() {
        auditoriaService = new AuditoriaServiceImpl(auditoriaRepository, usuarioRepository, auditoriaMapper, new ObjectMapper());
    }

    @Test
    void registrar_conUsuarioId_loResuelveYSerializaElDetalle() {
        Usuario usuario = Usuario.builder().id(1L).dni("30111222").build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(auditoriaRepository.save(any(Auditoria.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditoriaEvent event = new AuditoriaEvent(1L, "LOGIN_EXITOSO", "Usuario", 1L,
                Map.of("dni", "30111222"), "127.0.0.1", LocalDateTime.now());
        auditoriaService.registrar(event);

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isEqualTo(usuario);
        assertThat(captor.getValue().getAccion()).isEqualTo("LOGIN_EXITOSO");
        assertThat(captor.getValue().getDetalle()).contains("\"dni\"").contains("30111222");
        assertThat(captor.getValue().getIp()).isEqualTo("127.0.0.1");
    }

    @Test
    void registrar_conUsuarioIdNulo_persisteSinResolverUsuario() {
        AuditoriaEvent event = new AuditoriaEvent(null, "LOGIN_FALLIDO", "Usuario", null,
                Map.of("dni", "00000000"), "127.0.0.1", LocalDateTime.now());
        when(auditoriaRepository.save(any(Auditoria.class))).thenAnswer(inv -> inv.getArgument(0));

        auditoriaService.registrar(event);

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isNull();
        verify(usuarioRepository, never()).findById(any());
    }

    @Test
    void registrar_sinDetalle_noSerializaNada() {
        AuditoriaEvent event = new AuditoriaEvent(null, "LOGOUT", "RefreshToken", 5L,
                Map.of(), "127.0.0.1", LocalDateTime.now());
        when(auditoriaRepository.save(any(Auditoria.class))).thenAnswer(inv -> inv.getArgument(0));

        auditoriaService.registrar(event);

        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getDetalle()).isNull();
    }

    @Test
    void obtenerPorId_conIdInexistente_lanzaResourceNotFoundException() {
        when(auditoriaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditoriaService.obtenerPorId(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerPorId_existente_devuelveLaRespuestaMapeada() {
        Auditoria auditoria = Auditoria.builder().id(1L).accion("LOGIN_EXITOSO").entidad("Usuario").build();
        when(auditoriaRepository.findById(1L)).thenReturn(Optional.of(auditoria));
        when(auditoriaMapper.toResponse(auditoria)).thenReturn(
                new AuditoriaResponse(1L, null, null, "LOGIN_EXITOSO", "Usuario", null, null, "127.0.0.1", LocalDateTime.now()));

        AuditoriaResponse response = auditoriaService.obtenerPorId(1L);

        assertThat(response.accion()).isEqualTo("LOGIN_EXITOSO");
    }
}
