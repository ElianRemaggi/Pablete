package com.instituto.siga.exception;

import com.instituto.siga.dto.response.ErrorResponse;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/** A uniqueness conflict (DNI/email/codigo duplicado, docs/04 SS15). Maps to HTTP 409. */
@Getter
public class ConflictException extends RuntimeException {

    private final String codigo;

    public ConflictException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public ErrorResponse toErrorResponse() {
        return new ErrorResponse(codigo, getMessage(), LocalDateTime.now(), List.of());
    }
}
