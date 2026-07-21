package com.instituto.siga.service.impl;

import com.instituto.siga.dto.request.LoginRequest;
import com.instituto.siga.dto.request.RefreshTokenRequest;
import com.instituto.siga.dto.response.LoginResponse;
import com.instituto.siga.entity.RefreshToken;
import com.instituto.siga.entity.Rol;
import com.instituto.siga.entity.Usuario;
import com.instituto.siga.repository.RefreshTokenRepository;
import com.instituto.siga.repository.UsuarioRepository;
import com.instituto.siga.security.jwt.JwtProvider;
import com.instituto.siga.service.interfaces.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @Value("${app.jwt.refresh-token-exp-ms}")
    private long refreshTokenExpMs;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        try {
            // Delega la verificacion de password (BCrypt) y de cuenta habilitada al
            // DaoAuthenticationProvider auto-configurado por Spring a partir de
            // CustomUserDetailsService + PasswordEncoder.
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.dni(), request.password()));
        } catch (AuthenticationException ex) {
            // Normaliza el mensaje (Spring devuelve "Bad credentials" en ingles) sin
            // revelar si fallo el DNI o la contraseña.
            throw new BadCredentialsException("DNI o contraseña incorrectos");
        }

        Usuario usuario = usuarioRepository.findByDni(request.dni())
                .orElseThrow(() -> new BadCredentialsException("DNI o contraseña incorrectos"));

        return buildLoginResponse(usuario);
    }

    @Override
    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido o expirado"));

        if (refreshToken.isRevocado() || refreshToken.getExpiracion().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Refresh token inválido o expirado");
        }

        Usuario usuario = refreshToken.getUsuario();
        String newAccessToken = jwtProvider.generateAccessToken(usuario);

        return new LoginResponse(newAccessToken, refreshToken.getToken(), rolPrincipal(usuario), usuario.getNombre());
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(rt -> {
                    rt.setRevocado(true);
                    refreshTokenRepository.save(rt);
                });
    }

    private LoginResponse buildLoginResponse(Usuario usuario) {
        String accessToken = jwtProvider.generateAccessToken(usuario);

        RefreshToken refreshToken = RefreshToken.builder()
                .usuario(usuario)
                .token(generateOpaqueToken())
                .expiracion(LocalDateTime.now().plusNanos(refreshTokenExpMs * 1_000_000))
                .revocado(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return new LoginResponse(accessToken, refreshToken.getToken(), rolPrincipal(usuario), usuario.getNombre());
    }

    private String rolPrincipal(Usuario usuario) {
        return usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.joining(","));
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
