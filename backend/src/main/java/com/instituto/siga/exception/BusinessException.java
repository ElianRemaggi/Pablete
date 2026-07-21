package com.instituto.siga.exception;

import com.instituto.siga.dto.response.ErrorResponse;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/** A business rule violation (docs/07). Maps to HTTP 422 via GlobalExceptionHandler. */
@Getter
public class BusinessException extends RuntimeException {

    private final String codigo;
    private final List<String> detalles;

    public BusinessException(String codigo, String mensaje) {
        this(codigo, mensaje, List.of());
    }

    public BusinessException(String codigo, String mensaje, List<String> detalles) {
        super(mensaje);
        this.codigo = codigo;
        this.detalles = detalles;
    }

    public ErrorResponse toErrorResponse() {
        return new ErrorResponse(codigo, getMessage(), LocalDateTime.now(), detalles);
    }
}
