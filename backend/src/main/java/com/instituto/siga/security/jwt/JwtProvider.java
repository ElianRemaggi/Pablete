package com.instituto.siga.security.jwt;

import com.instituto.siga.entity.Rol;
import com.instituto.siga.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Emits and validates the short-lived JWT access token (docs/06-seguridad-autenticacion.md).
 * The refresh token is a separate, opaque, DB-persisted value (see RefreshToken entity) —
 * this class never issues or reads it.
 */
@Component
public class JwtProvider {

    private final SecretKey signingKey;
    private final long accessTokenExpMs;

    public JwtProvider(@Value("${app.jwt.secret}") String secret,
                        @Value("${app.jwt.access-token-exp-ms}") long accessTokenExpMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpMs = accessTokenExpMs;
    }

    public String generateAccessToken(Usuario usuario) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpMs);
        List<String> roles = usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.toList());

        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("dni", usuario.getDni())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    /**
     * @return parsed claims if the token is well-formed, signed by us, and not expired.
     * @throws JwtException if the token is invalid or expired.
     */
    public Claims parseAndValidate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException | io.jsonwebtoken.security.SecurityException
                 | io.jsonwebtoken.MalformedJwtException | io.jsonwebtoken.UnsupportedJwtException
                 | IllegalArgumentException e) {
            throw new JwtException("Token invalido o expirado", e);
        }
    }

    public Long getUsuarioId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(Claims claims) {
        return claims.get("roles", List.class);
    }
}
