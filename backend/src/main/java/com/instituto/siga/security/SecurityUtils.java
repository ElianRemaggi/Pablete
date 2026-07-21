package com.instituto.siga.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** El principal es directamente el usuarioId (Long) — lo setea JwtAuthFilter desde el token. */
    public static Long usuarioActualId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long usuarioId)) {
            return null;
        }
        return usuarioId;
    }
}
