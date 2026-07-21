package com.instituto.siga.service;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.LoginRequest;
import com.instituto.siga.dto.request.RefreshTokenRequest;
import com.instituto.siga.dto.response.LoginResponse;
import com.instituto.siga.entity.RefreshToken;
import com.instituto.siga.entity.Rol;
import com.instituto.siga.entity.Usuario;
import com.instituto.siga.repository.RefreshTokenRepository;
import com.instituto.siga.repository.UsuarioRepository;
import com.instituto.siga.security.jwt.JwtProvider;
import com.instituto.siga.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private AuditoriaPublisher auditoriaPublisher;

    private AuthServiceImpl authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(authenticationManager, usuarioRepository, refreshTokenRepository, jwtProvider, auditoriaPublisher);
        ReflectionTestUtils.setField(authService, "refreshTokenExpMs", 604800000L);

        usuario = Usuario.builder()
                .id(1L)
                .dni("30111222")
                .email("admin@instituto.edu.ar")
                .passwordHash("hash")
                .nombre("Ana")
                .apellido("Administradora")
                .activo(true)
                .roles(Set.of(Rol.builder().id(1L).nombre("ADMINISTRADOR").build()))
                .build();
    }

    @Test
    void login_conCredencialesValidas_devuelveAccessYRefreshToken() {
        when(usuarioRepository.findByDni("30111222")).thenReturn(Optional.of(usuario));
        when(jwtProvider.generateAccessToken(usuario)).thenReturn("access-token-123");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        LoginResponse response = authService.login(new LoginRequest("30111222", "Admin1234"));

        assertThat(response.accessToken()).isEqualTo("access-token-123");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.rol()).isEqualTo("ADMINISTRADOR");
        assertThat(response.nombre()).isEqualTo("Ana");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isEqualTo(usuario);
        assertThat(captor.getValue().isRevocado()).isFalse();
    }

    @Test
    void login_conCredencialesInvalidas_propagaAuthenticationException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("30111222", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        verify(usuarioRepository, times(0)).findByDni(any());
    }

    @Test
    void refresh_conTokenVigente_emiteNuevoAccessToken() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(10L)
                .usuario(usuario)
                .token("opaque-token")
                .expiracion(LocalDateTime.now().plusDays(1))
                .revocado(false)
                .build();
        when(refreshTokenRepository.findByToken("opaque-token")).thenReturn(Optional.of(refreshToken));
        when(jwtProvider.generateAccessToken(usuario)).thenReturn("new-access-token");

        LoginResponse response = authService.refresh(new RefreshTokenRequest("opaque-token"));

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("opaque-token");
    }

    @Test
    void refresh_conTokenRevocado_lanzaBadCredentials() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(10L)
                .usuario(usuario)
                .token("revoked-token")
                .expiracion(LocalDateTime.now().plusDays(1))
                .revocado(true)
                .build();
        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("revoked-token")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_conTokenExpirado_lanzaBadCredentials() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(10L)
                .usuario(usuario)
                .token("expired-token")
                .expiracion(LocalDateTime.now().minusMinutes(1))
                .revocado(false)
                .build();
        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("expired-token")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void logout_marcaElRefreshTokenComoRevocado() {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(10L).usuario(usuario).token("opaque-token")
                .expiracion(LocalDateTime.now().plusDays(1)).revocado(false).build();
        when(refreshTokenRepository.findByToken("opaque-token")).thenReturn(Optional.of(refreshToken));

        authService.logout(new RefreshTokenRequest("opaque-token"));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().isRevocado()).isTrue();
    }
}
