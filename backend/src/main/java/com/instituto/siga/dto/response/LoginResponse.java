package com.instituto.siga.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String rol,
        String nombre) {
}
