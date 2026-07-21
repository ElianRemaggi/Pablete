# 06. Seguridad y Autenticación

## 1. Esquema general

Autenticación **stateless** basada en **JWT**, con **Spring Security 6** configurando una cadena de filtros que valida el token en cada request. Autorización por rol y, en algunos endpoints, por propiedad del recurso (por ejemplo, un alumno sólo puede modificar sus propias inscripciones).

## 2. Flujo de autenticación

1. `POST /api/v1/auth/login` con `{dni, password}`.
2. `CustomUserDetailsService` carga el `Usuario` por DNI, `PasswordEncoder` (BCrypt) valida el hash.
3. Si es válido, `JwtProvider` genera:
   - **Access Token**: corta duración (15 minutos sugeridos), contiene `sub` (usuario id), `roles`, `iat`, `exp`.
   - **Refresh Token**: larga duración (7 días sugeridos), opaco o JWT, persistido en tabla `refresh_token` para poder revocarlo.
4. El cliente guarda el access token en memoria y usa el refresh token (idealmente en cookie `httpOnly`, `secure`, `sameSite=strict`) para renovarlo.
5. `POST /api/v1/auth/refresh` valida el refresh token contra la tabla (no revocado, no expirado) y emite un nuevo access token (rotación opcional del refresh token).
6. `POST /api/v1/auth/logout` marca el refresh token como `revocado = true`.

## 3. Configuración de Spring Security (resumen)

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
          .csrf(csrf -> csrf.disable()) // API stateless con JWT; CSRF no aplica a peticiones sin cookies de sesión
          .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(auth -> auth
              .requestMatchers("/api/v1/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
              .requestMatchers("/api/v1/administrador/**").hasRole("ADMINISTRADOR")
              .requestMatchers(HttpMethod.GET, "/api/v1/materias/**", "/api/v1/carreras/**").authenticated()
              .anyRequest().authenticated())
          .exceptionHandling(e -> e.authenticationEntryPoint(jwtAuthEntryPoint()))
          .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // factor de costo elevado
    }
}
```

> Nota: si en el futuro se incorpora autenticación basada en cookies de sesión para algún flujo, se reactivará protección CSRF específicamente para esos endpoints (`csrf.ignoringRequestMatchers` acotado, nunca deshabilitado global sin justificación).

## 4. Autorización por rol y por recurso

- Autorización gruesa (por rol) vía `@PreAuthorize("hasRole('ADMINISTRADOR')")` a nivel de método de servicio, además del filtro a nivel de URL.
- Autorización fina (por recurso) dentro del `Service`: por ejemplo, `InscripcionCursadaService.modificar(id, alumnoAutenticado)` verifica que la inscripción pertenezca al alumno autenticado antes de aplicar cambios, lanzando `AccessDeniedException` en caso contrario.

## 5. Hashing de contraseñas

- **BCrypt** con factor de costo 12 (ajustable según capacidad de hardware del servidor).
- Política de contraseña mínima validada en el DTO de alta/reseteo: longitud ≥ 8, al menos una mayúscula, un número.
- Endpoint de cambio de contraseña exige la contraseña actual antes de setear la nueva.

## 6. Rate limiting

- Filtro de rate limiting (por ejemplo con **Bucket4j** + Redis, o en memoria para instancia única) aplicado especialmente sobre:
  - `POST /auth/login` (mitigar fuerza bruta): ej. 5 intentos / minuto por IP+DNI.
  - Endpoints de generación de PDF/Excel (mitigar abuso de recursos de CPU).
- Respuesta `429 Too Many Requests` con `Retry-After`.

## 7. Protección CSRF

No aplica de forma tradicional porque la API es stateless y no usa cookies de sesión para autenticar requests mutantes; el propio uso de JWT en header `Authorization` ya mitiga CSRF clásico. Si se opta por guardar el refresh token en cookie `httpOnly`, se agrega:
- Cookie con `SameSite=Strict`.
- Verificación de header custom (`X-Requested-With`) en endpoints que dependan de esa cookie.

## 8. Validaciones de entrada

- Bean Validation (`jakarta.validation`) en todos los DTOs de request (`@NotBlank`, `@Email`, `@Min`, `@Max`, `@Pattern`).
- Sanitización adicional contra inyección en campos de texto libre (nombre, apellido) mediante whitelist de caracteres.
- Uso exclusivo de **JPA/Hibernate con parámetros nombrados** (nunca concatenación de SQL), lo que elimina el vector de SQL Injection por diseño.

## 9. Manejo global de excepciones

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity.unprocessableEntity().body(ex.toErrorResponse());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.toErrorResponse());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(
            "ACCESO_DENEGADO", "No tiene permisos para esta acción", LocalDateTime.now(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(
            "VALIDACION", "Datos de entrada inválidos", LocalDateTime.now(), detalles));
    }
}
```

## 10. Protección de datos personales

- Datos sensibles (DNI, email) nunca se exponen en logs de aplicación (uso de patrones de logging que enmascaran campos sensibles).
- Certificados PDF y exportaciones Excel se generan bajo demanda y no se almacenan permanentemente en disco del servidor (sólo se persiste su hash/metadata para trazabilidad en `certificado_emitido`).
- Comunicación exclusivamente sobre HTTPS/TLS en producción (terminación TLS en el proxy/Nginx del contenedor frontend o en un balanceador).

## 11. Siguiente etapa

Con el esquema de seguridad definido, el siguiente documento (`07-reglas-negocio-validaciones.md`) detalla exhaustivamente las reglas de negocio de cursadas y finales, y el catálogo completo de validaciones del sistema.
