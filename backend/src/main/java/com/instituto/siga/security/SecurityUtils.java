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

    /** true si el usuario autenticado tiene alguno de los roles indicados (sin el prefijo ROLE_). */
    public static boolean tieneAlgunRol(String... roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (String rol : roles) {
            for (var autoridad : auth.getAuthorities()) {
                if (("ROLE_" + rol).equals(autoridad.getAuthority())) {
                    return true;
                }
            }
        }
        return false;
    }
}
