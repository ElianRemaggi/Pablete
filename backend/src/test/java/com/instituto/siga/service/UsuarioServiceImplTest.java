package com.instituto.siga.service;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.UsuarioRequest;
import com.instituto.siga.dto.request.UsuarioRolesRequest;
import com.instituto.siga.dto.response.UsuarioResponse;
import com.instituto.siga.entity.Rol;
import com.instituto.siga.entity.Usuario;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.UsuarioMapper;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.RolRepository;
import com.instituto.siga.repository.UsuarioRepository;
import com.instituto.siga.service.impl.UsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private AlumnoRepository alumnoRepository;
    @Mock
    private UsuarioMapper usuarioMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditoriaPublisher auditoriaPublisher;

    private UsuarioServiceImpl usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioServiceImpl(usuarioRepository, rolRepository, alumnoRepository, usuarioMapper, passwordEncoder, auditoriaPublisher,
                org.mockito.Mockito.mock(com.instituto.siga.repository.RefreshTokenRepository.class));
        lenient().when(alumnoRepository.existsById(any())).thenReturn(false);
    }

    @Test
    void crear_conDniYaExistente_lanzaConflictException() {
        UsuarioRequest request = new UsuarioRequest("30111222", "nuevo@x.com", "N", "A", "Abcd1234", Set.of("ALUMNO"));
        when(usuarioRepository.existsByDni("30111222")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(request))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("DNI_DUPLICADO");
    }

    @Test
    void crear_conEmailYaExistente_lanzaConflictException() {
        UsuarioRequest request = new UsuarioRequest("30999888", "repetido@x.com", "N", "A", "Abcd1234", Set.of("ALUMNO"));
        when(usuarioRepository.existsByDni(any())).thenReturn(false);
        when(usuarioRepository.existsByEmail("repetido@x.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(request))
                .isInstanceOf(ConflictException.class)
                .extracting("codigo").isEqualTo("EMAIL_DUPLICADO");
    }

    @Test
    void crear_conRolInexistente_lanzaBusinessException() {
        UsuarioRequest request = new UsuarioRequest("30999888", "nuevo@x.com", "N", "A", "Abcd1234", Set.of("INEXISTENTE"));
        when(usuarioRepository.existsByDni(any())).thenReturn(false);
        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(usuarioMapper.toEntity(request)).thenReturn(new Usuario());
        when(rolRepository.findByNombre("INEXISTENTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.crear(request))
                .isInstanceOf(BusinessException.class)
                .extracting("codigo").isEqualTo("ROL_INVALIDO");
    }

    @Test
    void crear_conDatosValidos_hasheaPasswordYResuelveRoles() {
        UsuarioRequest request = new UsuarioRequest("30999888", "nuevo@x.com", "N", "A", "Abcd1234", Set.of("ALUMNO"));
        Usuario entidadMapeada = Usuario.builder().dni(request.dni()).email(request.email())
                .nombre(request.nombre()).apellido(request.apellido()).build();
        Rol rolAlumno = Rol.builder().id(1L).nombre("ALUMNO").build();

        when(usuarioRepository.existsByDni(any())).thenReturn(false);
        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(usuarioMapper.toEntity(request)).thenReturn(entidadMapeada);
        when(rolRepository.findByNombre("ALUMNO")).thenReturn(Optional.of(rolAlumno));
        when(passwordEncoder.encode("Abcd1234")).thenReturn("hash-seguro");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioMapper.toResponse(any(Usuario.class))).thenReturn(
                new UsuarioResponse(1L, "30999888", "nuevo@x.com", "N", "A", true, Set.of("ALUMNO"), null));

        usuarioService.crear(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash-seguro");
        assertThat(captor.getValue().getRoles()).containsExactly(rolAlumno);
        assertThat(captor.getValue().isActivo()).isTrue();
    }

    @Test
    void obtenerPorId_conIdInexistente_lanzaResourceNotFoundException() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerPorId(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizarRoles_resuelveNuevosRolesYLosGuarda() {
        Usuario usuario = Usuario.builder().id(1L).roles(Set.of()).build();
        Rol rolPreceptor = Rol.builder().id(2L).nombre("PRECEPTOR").build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombre("PRECEPTOR")).thenReturn(Optional.of(rolPreceptor));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(usuarioMapper.toResponse(any(Usuario.class))).thenReturn(
                new UsuarioResponse(1L, "d", "e", "n", "a", true, Set.of("PRECEPTOR"), null));

        usuarioService.actualizarRoles(1L, new UsuarioRolesRequest(Set.of("PRECEPTOR")));

        assertThat(usuario.getRoles()).containsExactly(rolPreceptor);
    }
}
