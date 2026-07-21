# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

SIGA (Sistema Web de Gestión Académica) replaces an institution's manual Google Forms + Excel process for cursada/final exam enrollment. The `docs/` folder is the pre-implementation design spec (written before any code existed); `backend/` is a working Spring Boot implementation actively being built out against that spec.

**Implemented and tested** (unit + MockMvc integration tests, verified against a running instance): JWT auth (login/refresh/logout), Usuario ABM, and full CRUD for Carrera, Materia, Correlatividad (with cycle detection), Comisión/Horario (with schedule-overlap detection), Mesa de Examen, ConfiguracionInstitucional (the institution-wide enrollment-window singleton), and a generic event-driven Auditoria trail wired into every one of the above.

**Not implemented yet**: PlanEstudio/PlanMateria, the Inscripción a Cursada/Final validator pipeline (docs/07 §1-2), estado académico, certificados (PDF), exportación (Excel), rate limiting, the entire frontend, and Docker/deploy. Don't assume any of these exist — check `backend/src/main/java/com/instituto/siga/` before referencing a class.

Read `docs/00-README.md` first for the recommended reading order across the design docs. Treat `docs/` as the authoritative contract for anything not yet built (exact fields, routes, roles, error codes) — implement what it says rather than re-deriving it, and flag it explicitly rather than silently deviating if a doc's decision looks wrong. For anything already built, the code is the source of truth (docs describe intent; the implementation sections below describe what's actually there, including where it deliberately diverges).

## Commands

Backend lives in `backend/`. Requires **JDK 17+** — check `java -version`; if it's not 17+, set `JAVA_HOME` before running Maven (this repo hit that exact issue: the machine's default `java` was 11, and PowerShell needs `$env:JAVA_HOME` set for the *current session*, not just installed). Maven itself doesn't need installing — use the wrapper (`mvnw.cmd` / `mvnw`).

```powershell
cd backend
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"; $env:Path = "$env:JAVA_HOME\bin;$env:Path"  # only if java -version isn't 17+

.\mvnw.cmd test              # unit tests only (Surefire)
.\mvnw.cmd verify            # unit + integration tests (Surefire + Failsafe — see note below)
.\mvnw.cmd test -Dtest=AuthServiceImplTest                      # single test class
.\mvnw.cmd test -Dtest=AuthServiceImplTest#login_conCredencialesValidas_devuelveAccessYRefreshToken  # single method
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8081   # run locally; pick a free port, don't assume 8080 is open
```

**Integration tests are named `*IT` and need the Failsafe plugin** (`mvn test` alone silently skips them — only `verify` runs both). This project hit that gap once already; if a new `*IT` class doesn't show up in test output, check `pom.xml` still has the `maven-failsafe-plugin` execution bound.

Dev profile (default, `application-dev.yml`) uses an **in-memory H2** DB seeded via `data.sql` with three demo users, all logging in by **DNI**, not username/email:
- `30111222` / `Admin1234` (ADMINISTRADOR)
- `30222333` / `Preceptor1234` (PRECEPTOR)
- `30333444` / `Alumno1234` (ALUMNO)

Prod profile (`application-prod.yml`) uses MySQL + Flyway (`src/main/resources/db/migration/V1__schema.sql`) — no demo users are seeded there, only the 3 roles and a placeholder `configuracion_institucional` row.

Swagger UI: `/swagger-ui.html` (bearerAuth scheme). H2 console (dev only): `/h2-console`.

## Ponytail plugin (installed, project scope)

This project has the [ponytail](https://github.com/DietrichGebert/ponytail) plugin installed (`.claude/settings.json`, scope: project) — it pushes toward the smallest solution that works (YAGNI → reuse → stdlib → native → existing dependency → one-liner → minimum custom code). It is compatible with this repo's spec, not a relaxation of it:

- The Clean Architecture layering, the composable validator pattern, and the event-published audit trail are **the spec**, not optional abstraction — ponytail's own rule is to never cut validation, error handling, security, or trust-boundary checks, so these stay mandatory.
- Where ponytail's ladder actually bites here: don't hand-roll what Spring/Hibernate/MapStruct provide, don't add config knobs or generic frameworks beyond what a given story needs, and don't scaffold speculative CRUD/service layers before a doc or ticket calls for them (e.g., PlanEstudio isn't built yet precisely because nothing needs it yet).

## Project skills (`.claude/skills/`)

- **siga-scaffold-module** — scaffolds a new academic-offering CRUD module (entity/DTO/mapper/repository/service/controller). The existing Carrera/Materia/Comision/MesaExamen modules are the reference implementations to match — same layer split, same `existsBy...AndIdNot`-style uniqueness checks, same `@PreAuthorize("hasRole('ADMINISTRADOR')")` placement on service methods (not controllers).
- **siga-add-validator** — for the (not-yet-built) enrollment validator pipeline described in `docs/07`.

## Architecture

**Clean Architecture**, unidirectional dependency (outer depends on inner, never reverse): controllers → services (interfaces + impl) → repositories/entities. DTOs+mappers sit between controllers and services.

**Actual backend package layout** (`com.instituto.siga`, see the directory for the current full list — it grows with each module):
```
config/              # CorsConfig, OpenApiConfig
security/jwt/        # JwtProvider, JwtAuthFilter, JwtAuthEntryPoint
security/config/     # SecurityConfig, PasswordEncoderConfig
security/userdetails/ # CustomUserDetailsService, SigaUserDetails
security/            # SecurityUtils.usuarioActualId() — reads the JWT-derived principal
audit/               # AuditoriaEvent (generic), AuditoriaPublisher, AuditoriaListener (@Async)
util/                # HttpRequestUtils.obtenerIpCliente() (X-Forwarded-For aware)
controller/          # One per resource, thin — bind request, call one service method, return DTO+status
service/interfaces/ + service/impl/
repository/          # Spring Data JPA; custom @Query only for filtered/paginated searches or graph queries
dto/request/ + dto/response/
mapper/              # MapStruct; toEntity() ignores fields the service must resolve itself (FKs, computed state)
entity/
exception/           # BusinessException (422), ConflictException (409), ResourceNotFoundException (404), GlobalExceptionHandler
```

**Strict rule**: controllers never contain business logic, validation, or repository calls. Role checks live on **service** methods via `@PreAuthorize("hasRole('ADMINISTRADOR')")`, not on controllers or as extra `SecurityConfig` URL patterns — `SecurityConfig` only distinguishes public (`/auth/login`, `/auth/refresh`, swagger, h2-console) from "must be authenticated"; per-role restriction is method-level.

**Frontend**: not started. `docs/05-modulos-frontend.md` has the intended React/TS layout and route tree if/when that begins.

## Domain model — what's live vs. spec-only

Full ER model and DDL in `docs/02-modelo-datos.md`; entity/DTO shapes in `docs/03-entidades-dto.md`. Entities that exist in code today: `Usuario`, `Rol`, `RefreshToken`, `Carrera`, `Materia`, `Correlatividad` (+ `TipoRequisito`), `Comision`, `Horario`, `MesaExamen` (+ `Turno`), `ConfiguracionInstitucional`, `Auditoria`. Everything else in the DDL (`plan_estudio`, `plan_materia`, `inscripcion_cursada`, `inscripcion_final`, `intento_final`, `estado_academico_materia`, `certificado_emitido`) is schema-only in `V1__schema.sql` — no JPA entity maps to it yet, so don't import a class for it.

- `Comision`/`Horario` have no `activa` flag in the DDL — `eliminar()` is a real delete, unlike `Carrera`/`Materia` which soft-delete via `activa=false`.
- `ConfiguracionInstitucional` is a true singleton (`id` fixed to `1`); both `data.sql` and `V1__schema.sql` seed a default row with relative dates (`DATEADD`/`DATE_ADD` from "now") so the enrollment window starts open — don't hardcode absolute seed dates, they go stale.
- Correlatividad cycle detection (`CorrelatividadServiceImpl.creariaCiclo`) does a BFS over all existing edges fetched via a lightweight `(materiaId, materiaRequeridaId)` projection query, not full entity hydration.
- Comisión schedule-overlap validation is scoped to *within one comisión* — two different comisiones of the same materia are allowed to share a time slot; cross-comisión overlap for a given student is a future Inscripción-pipeline concern, not this layer's.

## Auditing — actual implementation (docs/08-auditoria.md)

One generic `AuditoriaEvent` record (not a class per action) carries `usuarioId, accion, entidad, entidadId, detalle, ip, fecha`; every sensitive service method calls `AuditoriaPublisher.publicar(...)` after its mutation succeeds. `AuditoriaListener` picks it up `@Async` via `@TransactionalEventListener(phase = AFTER_COMPLETION, fallbackExecution = true)`.

**AFTER_COMPLETION, not AFTER_COMMIT** — deliberate, and non-obvious: `AuthServiceImpl.login()` publishes a `LOGIN_FALLIDO` audit event and then rethrows, which rolls back that (read-only) transaction. `AFTER_COMMIT` would silently never fire for failed-login audits — exactly the events most worth keeping. `AFTER_COMPLETION` fires regardless of commit/rollback; the tradeoff (a create/update audit could theoretically fire even if a later, unrelated commit failure rolled back that write) was judged acceptable against losing failed-login trazability, per docs §1's stated priority.

`ConfiguracionInstitucionalServiceImpl.actualizar()` builds a real before/after diff per changed field (`{"campo": {"antes": ..., "despues": ...}}`), matching the literal example in docs §3 — every other service's audit `detalle` is just the new/final field values, not a diff (docs only demands the antes/despues shape for config changes specifically).

`Auditoria.detalle` is a `String` column (`columnDefinition = "json"`) holding Jackson-serialized JSON, not a native mapped type — no extra library needed, MySQL/H2 both accept a valid JSON string through a plain `VARCHAR`/`JSON` column. It comes back from the API as an escaped JSON string (not inlined as a nested object) — the frontend is expected to `JSON.parse()` it (docs §5: "se muestra formateado... en un panel expandible").

`GET /api/v1/auditoria` (filters: `usuarioId`, `accion`, `entidad`, `fechaDesde`, `fechaHasta`; paginated, `fecha DESC` always) and `GET /api/v1/auditoria/{id}` are ADMIN-only. Nothing ever deletes or updates an `Auditoria` row (docs §6, append-only) — don't add such an endpoint.

## Security (docs/06-seguridad-autenticacion.md)

- Stateless JWT: short-lived access token (claims: `sub`=usuarioId, `roles`, no username/dni beyond that) + opaque, DB-persisted, revocable refresh token. Login is by **DNI**, not email/username.
- `JwtAuthFilter` sets the `Authentication` principal to the **raw `usuarioId` (Long)**, not a `UserDetails` object — that's why `SecurityUtils.usuarioActualId()` casts the principal directly, and why `@AuthenticationPrincipal Long usuarioId` (not a custom UserDetails type) is the right controller parameter for "current user" (see `UsuarioController.obtenerActual`).
- BCrypt cost 12. CSRF intentionally disabled (stateless, header-based auth, no session cookies) — don't re-enable without the cookie-based-refresh-token justification in docs §3/§7.
- `server.servlet.encoding.force: true` is set deliberately — without it, some HTTP clients (confirmed with Windows PowerShell 5.1's `Invoke-RestMethod`) mis-decode UTF-8 response bodies because Spring didn't declare a charset in `Content-Type`. Don't remove it.

## API conventions (docs/04-api-endpoints.md)

All endpoints under `/api/v1`. Every error follows `ErrorResponse{codigo, mensaje, timestamp, detalles}` via `GlobalExceptionHandler`. Actual codes in use today (not the aspirational inscripción-pipeline ones from docs §15, which don't exist yet since that module isn't built): `VALIDACION` (400), `CREDENCIALES_INVALIDAS`/`NO_AUTENTICADO` (401), `ACCESO_DENEGADO` (403), `RECURSO_NO_ENCONTRADO` (404), `*_DUPLICADO`/`*_DUPLICADA` (409, one per entity: `DNI_DUPLICADO`, `EMAIL_DUPLICADO`, `CARRERA_DUPLICADA`, `MATERIA_DUPLICADA`, `CORRELATIVIDAD_DUPLICADA`, `COMISION_DUPLICADA`, `MESA_DUPLICADA`), business-rule violations at 422 (`ROL_INVALIDO`, `CORRELATIVIDAD_AUTOREFERENCIA`, `CORRELATIVIDAD_CICLICA`, `HORARIO_INVALIDO`, `HORARIO_SUPERPUESTO`, `MESA_FUERA_DE_FECHA`, `CONFIGURACION_FECHAS_INVALIDAS`), `ERROR_INTERNO` (500, logged server-side — `GlobalExceptionHandler`'s catch-all must always log before returning the generic body, or failures become undiagnosable).

## Definition of Done

Every change should include: code in the correct layer (no logic in controllers), Bean Validation on request DTOs + explicit business validation in the service for anything Bean Validation can't express, errors mapped to the conventions above, a unit test per new business rule plus a MockMvc `*IT` test for role enforcement, an audit event for anything sensitive, and — since this has bitten before — an actual `mvnw verify` run plus a live `spring-boot:run` + curl/PowerShell check before calling something done. Tests passing has not been a reliable signal by itself in this project; several real bugs (data.sql load-order, H2-vs-MySQL SQL dialect differences, the audit transaction-phase bug, `Map.of()` NPEs on null-ish test fixtures) only showed up when actually running the app.
