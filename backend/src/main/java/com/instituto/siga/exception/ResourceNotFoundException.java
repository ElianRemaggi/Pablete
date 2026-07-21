package com.instituto.siga.exception;

/** A referenced resource does not exist. Maps to HTTP 404 via GlobalExceptionHandler. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }
}
