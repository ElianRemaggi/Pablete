# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

This repository currently contains **only design documentation** (`docs/`) for SIGA (Sistema Web de Gestión Académica) — no source code has been written yet. It replaces an institution's manual Google Forms + Excel process for cursada/final exam enrollment with a full web system. Per `docs/10-roadmap-entregables.md`, this is **Etapa 1 de 6 (Análisis y arquitectura)** — the project is at the design stage; backend/frontend scaffolding, implementation, hardening, and deployment are all still pending stages.

Read `docs/00-README.md` first — it indexes all other docs and gives the recommended reading order: architecture → data model → backend (entities → API) → frontend → security, business rules, audit, diagrams, roadmap.

## Ponytail plugin (installed, project scope)

This project has the [ponytail](https://github.com/DietrichGebert/ponytail) plugin installed (`.claude/settings.json`, scope: project) — it pushes toward the smallest solution that works (YAGNI → reuse → stdlib → native → existing dependency → one-liner → minimum custom code). It is compatible with this repo's spec, not a relaxation of it:

- The Clean Architecture layering, the composable validator pattern (`ValidadorInscripcionCursada`/`ValidadorInscripcionFinal` families), and the event-published audit trail are **the spec**, not optional abstraction — ponytail's own rule is to never cut validation, error handling, security, or trust-boundary checks, so these stay mandatory.
- Where ponytail's ladder actually bites here: don't hand-roll what Spring/Hibernate/MapStruct/MUI already provide, don't add config knobs or generic frameworks beyond what a given story needs, and don't scaffold speculative CRUD/service layers before a doc or ticket calls for them.

## Project skills (`.claude/skills/`)

Two skills encode the repo's most repetitive, precisely-specified backend patterns so they get followed consistently once implementation starts (Etapa 3-4 in the roadmap):

- **siga-scaffold-module** — scaffolds a new academic-offering CRUD module (entity/DTO/mapper/repository/service/controller) matching the layout in `docs/01` and the exact fields/endpoints in `docs/02`-`docs/04`.
- **siga-add-validator** — adds a new enrollment business rule as a `@Component` in the Strategy/Chain of Responsibility pipeline described in `docs/07`, with the right error code and audit event.

Both exist because the docs already pin down the exact contract (fields, routes, roles, error codes) — the skill's job is making sure that contract gets copied faithfully instead of re-derived from scratch each time. No skills were added yet for the frontend or for cross-cutting concerns (security config, PDF/Excel generation) since those don't have a repeating per-entity shape the same way — add one if that changes.

When asked to start implementing, treat the `docs/` files as the authoritative spec — they were written to be unambiguous enough to build directly from (per the closing note in `10-roadmap-entregables.md`). Don't redesign what's already decided there; implement it. If a doc's decision seems wrong, flag it rather than silently deviating.

## Architecture (as specified in docs/01-analisis-arquitectura.md)

**Clean Architecture** with strict unidirectional dependency (outer layers depend on inner, never the reverse): Frameworks & Drivers (controllers, security config, JPA persistence, PDF/Excel libs, React web) → Interface Adapters (DTOs, mappers, repository impls) → Application/Use Cases (services) → Domain/Entities.

**Backend package layout** (`com.instituto.siga`):
```
config/          # Beans, CORS, OpenAPI/Swagger, Jackson, Async
security/jwt/    # JwtProvider, JwtFilter, JwtAuthEntryPoint
security/config/ # SecurityConfig, PasswordEncoderConfig
security/userdetails/
controller/      # One per resource — thin, delegates to service interfaces only
service/interfaces/ + service/impl/   # All business logic and orchestration lives here
repository/      # Spring Data JPA
dto/request/ + dto/response/
mapper/          # MapStruct Entity <-> DTO
entity/          # JPA entities
exception/       # GlobalExceptionHandler, BusinessException, ResourceNotFoundException, ValidationException
validation/      # Custom validators (@Correlatividad, @HorarioValido, etc.)
util/
audit/           # Auditing interceptors/aspects
```

**Strict rule**: Controllers only receive the request, delegate to the corresponding Service via its interface, and return the response DTO with the right HTTP status. No business logic, domain validation, or repository access belongs in a controller.

**Frontend layout** (`src/`, React + TypeScript):
```
app/             # store, router, theme
api/             # Axios client + interceptors (JWT, refresh)
auth/            # Auth context, useAuth
modules/login/, modules/alumno/, modules/preceptor/, modules/administrador/
components/      # Shared (Navbar, Sidebar, Table, Modal, Alert)
hooks/, types/, utils/, theme/
```

Routes are grouped by role (`/alumno/*`, `/preceptor/*`, `/administrador/*`) and gated by a `RequireRole(rol)` component that reads the decoded JWT from the auth context and redirects to `/403` on mismatch. Full route tree is in `docs/05-modulos-frontend.md`.

**Design patterns in use**: Strategy + Chain of Responsibility (composable enrollment validators — see below), Factory (`CertificadoFactory`), Builder (`ExcelReportBuilder`), Facade (`InscripcionFacadeService`), Observer/event-driven (Spring `ApplicationEventPublisher` decouples audit logging from business services), Repository, DTO+Mapper (MapStruct).

## Stack

- **Backend**: Java 17+, Spring Boot 3.x, Spring Security 6, JWT (jjwt), Hibernate/JPA, Maven, Lombok, MapStruct, OpenPDF/iText (PDF), Apache POI (Excel).
- **Frontend**: React 18, TypeScript, Vite, Material UI, React Router, Axios, React Hook Form + Yup/Zod.
- **Database**: MySQL 8 (full DDL, indexes, and seed data in `docs/02-modelo-datos.md`).
- **Deployment**: Docker Compose (mysql, backend :8080, frontend/nginx :80/443, optional redis for rate limiting). Config externalized via `application.yml` + `.env` (DB credentials, JWT secret, token TTLs, CORS origins).
- **Git branching**: simplified GitFlow — `main`, `develop`, `feature/*`, `release/*`, `hotfix/*`.

## Domain model essentials

Full ER model and DDL in `docs/02-modelo-datos.md`; JPA entities/DTOs/mappers in `docs/03-entidades-dto.md`. Key relationships to keep in mind when touching any academic-flow code:

- `usuario` has role(s) via `usuario_rol` (N:M), with 0..1 each of `alumno`/`preceptor`/`administrador` (table-per-subclass).
- `carrera` → `plan_estudio` → `materia` (via `plan_materia`, since a materia can belong to multiple plans).
- `materia` correlatividades are self-referential (`materia_id` / `materia_requerida_id`), typed `CURSAR` vs `APROBAR`.
- Enrollment flows: `comision`/`horario` → `inscripcion_cursada`; `mesa_examen` → `inscripcion_final` → `intento_final` (attempt history per materia).
- `estado_academico_materia` is the single source of truth per (alumno, materia) — `PENDIENTE/CURSANDO/REGULAR/APROBADA/LIBRE` — never duplicate/derive this elsewhere.
- `configuracion_institucional` is a singleton row (enrollment date windows, max final-exam attempts, hours-before-blocked) — all business rule thresholds must read from here, never be hardcoded.
- `auditoria` and `refresh_token` support traceability and JWT session revocation respectively.

## Business rules (docs/07-reglas-negocio-validaciones.md)

Enrollment logic is implemented as **composable validators** (Strategy + Chain of Responsibility), injected as an ordered list into the service rather than as inline if-chains:

- **Cursada enrollment** (`ValidadorInscripcionCursada` implementations run in order): correlatividades satisfied → no schedule overlap with the student's other active enrollments this period → cupo available → within the enrollment date window → no duplicate enrollment.
- **Final exam enrollment** (`ValidadorInscripcionFinal`-style): materia is `REGULAR`/`APROBADA` → no other active enrollment for a mesa at the same date+turno → attempt count under `configuracion.limiteIntentosFinal` → at least `configuracion.horasBloqueoFinal` (48h default) before the mesa → within the finales enrollment window → no duplicate.
- Modifying an enrollment re-runs the same validators against the new target; dropping an enrollment is a soft-delete (`estado = BAJA`) preserved for audit.
- Any new validation rule should be added as its own `@Component` implementing the relevant validator interface, not folded into an existing one — this is the extension point the architecture is built around.
- All thresholds/dates driving these rules come from `configuracion_institucional`, editable only by ADMINISTRADOR, with every change audited (before/after values).

## Security (docs/06-seguridad-autenticacion.md)

- Stateless JWT auth: short-lived access token + longer-lived refresh token persisted in `refresh_token` (so it can be revoked). Login is by **DNI + password**, not email.
- BCrypt (cost factor 12). CSRF is intentionally disabled (`csrf.disable()`) because the API is stateless and doesn't use session cookies — don't "fix" this by re-enabling it without the cookie-based refresh-token justification noted in that doc.
- Role authorization enforced twice: coarse-grained at the URL filter chain (`SecurityConfig`) and again via `@PreAuthorize` at the service method; resource-level ownership checks (e.g., a student can only modify their own enrollment) happen inside the service, throwing `AccessDeniedException`.
- Rate limiting is required on `/auth/login` and PDF/Excel generation endpoints (429 + `Retry-After`).

## Auditing (docs/08-auditoria.md)

Audit records are written via Spring application events (`ApplicationEventPublisher` → `@Async @EventListener`), never by calling the audit service directly from business logic — this keeps `Service` classes decoupled from how/where auditing happens. Every sensitive action (auth events, user/role changes, all enrollment mutations, academic status changes, academic-offering CRUD, config changes, exports, certificate downloads) must publish a corresponding event.

## API conventions (docs/04-api-endpoints.md)

All endpoints under `/api/v1`. Errors use a consistent `ErrorResponse` shape with semantic HTTP status codes (400/401/403/404/409/422) — business rule violations return 422 with a specific error code (e.g. `REGLA_NEGOCIO_CORRELATIVIDAD`, `CUPO_AGOTADO`, `LIMITE_INTENTOS_SUPERADO`, `FUERA_DE_PLAZO`), per the acceptance criteria in `docs/10-roadmap-entregables.md`.

## Definition of Done (docs/10-roadmap-entregables.md)

Every user story/feature is expected to include: code in the correct Clean Architecture layer, input validation (Bean Validation) and business validation (dedicated validators), errors mapped to the API error catalog, unit tests for service-layer rules plus integration tests (MockMvc / Testcontainers with MySQL), an audit event where the action is sensitive, and Swagger/OpenAPI documentation for the endpoint.
