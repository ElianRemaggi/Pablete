# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

SIGA (Sistema Web de Gestión Académica) replaces an institution's manual Google Forms + Excel process for cursada/final exam enrollment. The `docs/` folder is the pre-implementation design spec (written before any code existed); `backend/` is a working Spring Boot implementation actively being built out against that spec.

**Implemented and tested** (unit + MockMvc integration tests, verified against a running instance): JWT auth (login/refresh/logout), Usuario ABM, full CRUD for Carrera, Materia (+ Plan de Estudio / Plan-Materia association and filtering), Correlatividad (with cycle detection), Comisión/Horario (with schedule-overlap detection), Mesa de Examen, ConfiguracionInstitucional (the institution-wide enrollment-window singleton), the full Inscripción a Cursada and Inscripción a Final validator pipelines (docs/07 §1-2), Alumno lookup + historial académico, Certificados PDF (OpenPDF), Exportación Excel (Apache POI), and a generic event-driven Auditoria trail wired into every one of the above. This is the complete MVP backend per `docs/10-roadmap-entregables.md` §2 "Must have"/"Should have".

**Not implemented yet**: rate limiting, the entire frontend, Docker/deploy, reports-with-charts (Should-have), and everything under "Could have"/"Won't have" in docs/10 §2. Don't assume any of these exist — check `backend/src/main/java/com/instituto/siga/` before referencing a class.

Read `docs/00-README.md` first for the recommended reading order across the design docs. Treat `docs/` as the authoritative contract for anything not yet built (exact fields, routes, roles, error codes) — implement what it says rather than re-deriving it, and flag it explicitly rather than silently deviating if a doc's decision looks wrong. For anything already built, the code is the source of truth (docs describe intent; the implementation sections below describe what's actually there, including where it deliberately diverges).

**Identity/calendar/comprobante update** (post-MVP, driven by `Prompt para actualizar SIGA.pdf`): institutional palette based on `#05acec` (`frontend/src/theme.ts`), the ISET logo (`frontend/src/assets/logo-iset.jpg`, `frontend/public/logo-iset.jpg`, `backend/src/main/resources/logo-iset.jpg`) integrated into login/dashboards/PDFs, a calendar (`CalendarioService`/`CalendarioController`, `/api/v1/calendario/*`) that **derives** cursada/final/inscripción events live from `horario`, `mesa_examen`, `inscripcion_cursada`/`inscripcion_final` and `configuracion_institucional` — only genuinely new institutional events/deadlines live in the new `evento_academico` table (`EventoAcademicoService`, `/api/v1/eventos-academicos`) — an institutional inscription-receipt PDF (`CertificadoFactory.generarComprobanteCursada/Final`, new `TipoCertificado.COMPROBANTE_CURSADA/FINAL`), and a `profesor` column added to `comision`/`mesa_examen` (nullable — there is no `Profesor`/`Docente` entity in this codebase, and none was added; it's a plain text field). `Alumno` still has no JPA-mapped carrera (the `alumno_carrera` table in the DDL remains unmapped) — carrera on a receipt/ficha is resolved per-materia via `PlanMateriaRepository.buscarCarreraVigentePorMateria`, not a single field on the student. `V2__calendario_profesor_comprobantes.sql` is the Flyway migration for this (prod only — dev's H2 `create-drop` picks up the new entity/columns automatically). No numeric grade ("nota") exists anywhere in the model; the ficha shows only `EstadoAcademicoMateria.estado` and `IntentoFinal.resultado`, per an explicit scope decision, not an oversight.

## Primera entrega — exámenes finales + Excel (post-MVP, driven by `Prompt_SIGA_Examenes_Finales_y_Excel.pdf`)

Read `docs/12-primera-entrega-examenes-finales.md` (alcance, ciclo de vida de la mesa, endpoints, cómo probar) and `docs/11-historial-excel.md` (Excel) before touching these areas. Key facts that are **not obvious from the code**:

- **Mesa lifecycle**: `MesaExamen.estado` (`EstadoMesa`: BORRADOR→PUBLICADA⇄CERRADA, CANCELADA). Mesas created through the API are born in BORRADOR (the entity builder defaults to PUBLICADA only to keep pre-existing rows/tests valid; `V4__mesa_examen_estado_ventana.sql` defaults old rows to PUBLICADA). Alumnos only see PUBLICADA mesas (`MesaExamenServiceImpl.listar/obtener`, role read from the SecurityContext via `SecurityUtils.tieneAlgunRol`). Cancelling a mesa sets active inscriptions to BAJA and frees their `IntentoFinal`; nothing is deleted. Any IT that needs an enrollable mesa must call `PATCH /mesas-examen/{id}/publicar` after creating it.
- **Final-enrollment pipeline** adds `ValidadorMesaDisponible` (@Order 0, estado PUBLICADA) and `ValidadorCupoFinal` (@Order 6); `ValidadorVentanaInscripcionFinal` now goes through `VentanaInscripcionMesa` (mesa's own apertura/cierre if both set, else the global `ConfiguracionInstitucional` window). `InscripcionFinalServiceImpl` takes a PESSIMISTIC_WRITE lock on the mesa (`MesaExamenRepository.findByIdForUpdate`) before validating so concurrent enrollments can't exceed the cupo. `/inscripciones-final/me` returns the full history including BAJA.
- **Excel** (`excel/`): `HistorialExcelPath` anchors the relative default path to the project root regardless of whether the backend starts from `backend/` or the root; `HistorialExcelBootstrap` creates an empty file from `resources/historial-academico-plantilla.xlsx` only when missing; `HistorialExcelWriter.actualizarCelda` returns a `Resultado` (not a boolean) and `fijarEstado`'s response carries `excelSincronizado/excelMensaje`; sync conflicts keep an APROBADA already in the DB. `/data/` is gitignored (real student data); the template at `data/historial-academico.xlsx` has the original 9 sheets/headers and no students. Excel data rows start at index 2 (Excel row 3).
- **Auth**: public `POST /auth/registro` (only creates ALUMNO; `RegistroRequest` has no roles field), `PUT /usuarios/me` (email only).
- **Cursadas hidden, not removed**: frontend flag `VITE_CURSADAS_HABILITADAS` (`frontend/src/config/features.ts`) gates menu items and routes; backend endpoints for cursadas remain active.
- Tooling gotcha seen here: some source files have mixed CRLF/LF line endings — normalise before scripted string replaces.

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

## Docker

`docker-compose.yml` (repo root) + `backend/Dockerfile` + `frontend/Dockerfile` run the whole system with `docker compose up --build` — zero local Java/Node/DB install required. Deliberately runs the backend on the **`dev` profile** (in-memory H2, demo users pre-seeded), not `prod`+MySQL: the goal is "anyone can try the full app in one command," and `prod` seeds no demo users at all (see above), which would leave a fresh container with no way to log in. `frontend/Dockerfile` bakes `VITE_API_BASE_URL` in at build time via `ARG`/`ENV` (ports: backend `8080`, frontend `8081` via nginx) — if those port mappings in `docker-compose.yml` change, `CORS_ALLOWED_ORIGINS` (backend env) and the `VITE_API_BASE_URL` build arg (frontend) both need to move with them, or login will fail with a CORS error. `frontend/nginx.conf` does the SPA fallback (`try_files ... /index.html`) React Router needs — without it, refreshing on any non-root route (e.g. `/alumno/dashboard`) 404s. A MySQL-backed `prod`-profile compose variant for a real staging environment is intentionally not built yet (docs/10 §1 Etapa 8 still lists CI/CD and staging as pending) — don't assume one exists.

**Verified on a real machine with Docker** (this dev environment has no Docker installed, so it couldn't be tested here): both images build successfully end-to-end with `docker compose up --build`. One real bug surfaced and was fixed in the process — `frontend/package-lock.json` was out of sync with `package.json` (missing optional/transitive entries like `@emnapi/core`/`@emnapi/runtime` that the Vite/Rolldown dependency chain now needs), which made the Dockerfile's `npm ci` step fail; regenerating the lockfile via `npm install` fixed it without touching `package.json` or any source. Full run-to-login hasn't been confirmed yet — the test machine hit a "port 8081 already allocated" conflict (an unrelated port already in use on that machine, not a bug here) before getting to the browser/login step.

## Ponytail plugin (installed, project scope)

This project has the [ponytail](https://github.com/DietrichGebert/ponytail) plugin installed (`.claude/settings.json`, scope: project) — it pushes toward the smallest solution that works (YAGNI → reuse → stdlib → native → existing dependency → one-liner → minimum custom code). It is compatible with this repo's spec, not a relaxation of it:

- The Clean Architecture layering, the composable validator pattern, and the event-published audit trail are **the spec**, not optional abstraction — ponytail's own rule is to never cut validation, error handling, security, or trust-boundary checks, so these stay mandatory.
- Where ponytail's ladder actually bites here: don't hand-roll what Spring/Hibernate/MapStruct provide, don't add config knobs or generic frameworks beyond what a given story needs, and don't scaffold speculative CRUD/service layers before a doc or ticket calls for them.

## Project skills (`.claude/skills/`)

- **siga-scaffold-module** — scaffolds a new academic-offering CRUD module (entity/DTO/mapper/repository/service/controller). The existing Carrera/Materia/Comision/MesaExamen modules are the reference implementations to match — same layer split, same `existsBy...AndIdNot`-style uniqueness checks, same `@PreAuthorize("hasRole('ADMINISTRADOR')")` placement on service methods (not controllers).
- **siga-add-validator** — for adding a new `ValidadorInscripcionCursada`/`ValidadorInscripcionFinal` to the enrollment pipelines described in `docs/07`. `ValidadorSuperposicionHoraria`, `ValidadorCorrelatividad`, `ValidadorCupoDisponible`, `ValidadorVentanaInscripcion` (cursada) and `ValidadorRegularidad`, `ValidadorSuperposicionMesa`, `ValidadorIntentosMaximos`, `ValidadorVentanaInscripcionFinal`, `ValidadorAntelacionMinima` (final) are the reference implementations — `@Order` + `@Component`, injected as `List<X>` (auto-ordered by Spring).

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
validation/          # ValidadorInscripcionCursada / ValidadorInscripcionFinal Strategy implementations (@Order)
certificado/         # CertificadoFactory (Factory pattern, docs/01 SS9)
exportacion/         # ExcelReportBuilder (Builder pattern, docs/01 SS9)
controller/          # One per resource, thin — bind request, call one service method, return DTO+status
service/interfaces/ + service/impl/
repository/          # Spring Data JPA; custom @Query only for filtered/paginated searches or graph queries
dto/request/ + dto/response/
mapper/              # MapStruct; toEntity() ignores fields the service must resolve itself (FKs, computed state)
entity/
exception/           # BusinessException (422), ConflictException (409), ResourceNotFoundException (404), GlobalExceptionHandler
```

**Strict rule**: controllers never contain business logic, validation, or repository calls. Role checks live on **service** methods via `@PreAuthorize("hasRole('ADMINISTRADOR')")`, not on controllers or as extra `SecurityConfig` URL patterns — `SecurityConfig` only distinguishes public (`/auth/login`, `/auth/refresh`, swagger, h2-console) from "must be authenticated"; per-role restriction is method-level.

**Frontend**: `frontend/` is a Vite + React + TypeScript app implementing the route tree and stack from `docs/05-modulos-frontend.md` (MUI, React Router, React Hook Form + Zod, Axios, TanStack Query, recharts). Auth (login, JWT-decoded `AuthContext`, protected `RequireRole` routing, axios refresh-on-401 interceptor) and every route/layout are wired for all three roles. Fully implemented against the real backend, with no known gaps: Carreras, Materias, Correlatividades, Comisiones+Horarios, Mesas de Examen, Planes de Estudio, Usuarios, Configuración, Auditoría, and two Reportes charts — ocupación de cupos and tasa de aprobación por materia (ADMINISTRADOR); búsqueda de alumnos + ficha (estado académico + inscripciones a cursada/final) + exportaciones (PRECEPTOR); dashboard, perfil, historial académico, inscripción a cursada/final, certificados (ALUMNO). `/administrador/horarios` is a deliberate stub pointing at Comisiones — the backend has no standalone horario resource (horarios are managed via `POST/DELETE /comisiones/{id}/horarios/...`), so a separate CRUD screen there would misrepresent the API. Building the frontend surfaced two real backend gaps, since closed: `GET /inscripciones-cursada|final/alumno/{id}` (PRECEPTOR/ADMIN, powers the Ficha de Alumno tabs) and `GET /reportes/tasa-aprobacion-por-materia` (new `ReporteController`/`ReporteService`, aggregates `EstadoAcademicoMateriaRepository` via a grouped `Object[]` projection — same pattern as `CorrelatividadRepository.buscarAristas()`). Both are additions beyond `docs/04`'s original spec, documented there under §8/§10/§15. Run with `cd frontend && npm run dev` (defaults to `http://localhost:5173`, CORS-allowed by `application.yml`'s `app.cors.allowed-origins` default); point `VITE_API_BASE_URL` (`.env.development.local`, gitignored) at whatever port the backend actually runs on if 8080 is taken.

## Domain model — what's live vs. spec-only

Full ER model and DDL in `docs/02-modelo-datos.md`; entity/DTO shapes in `docs/03-entidades-dto.md`. Every entity in the DDL now has a matching JPA entity: `Usuario`, `Rol`, `RefreshToken`, `Carrera`, `PlanEstudio`, `PlanMateria` (+ `PlanMateriaId` composite key), `Materia`, `Correlatividad` (+ `TipoRequisito`), `Comision`, `Horario`, `MesaExamen` (+ `Turno`), `Alumno`, `EstadoAcademicoMateria` (+ `EstadoMateria`), `InscripcionCursada`, `InscripcionFinal`, `IntentoFinal` (+ `EstadoInscripcion`, `ResultadoFinal`), `CertificadoEmitido` (+ `TipoCertificado`), `ConfiguracionInstitucional`, `Auditoria`. `Preceptor`/`Administrador` (1:1 subtype tables in the DDL) are still schema-only — nothing needs them as distinct entities yet, since role checks work off `Usuario.roles`.

- `Comision`/`Horario` have no `activa` flag in the DDL — `eliminar()` is a real delete, unlike `Carrera`/`Materia` which soft-delete via `activa=false`.
- `ConfiguracionInstitucional` is a true singleton (`id` fixed to `1`); both `data.sql` and `V1__schema.sql` seed a default row with relative dates (`DATEADD`/`DATE_ADD` from "now") so the enrollment window starts open — don't hardcode absolute seed dates, they go stale.
- Correlatividad cycle detection (`CorrelatividadServiceImpl.creariaCiclo`) does a BFS over all existing edges fetched via a lightweight `(materiaId, materiaRequeridaId)` projection query, not full entity hydration.
- Comisión schedule-overlap validation at the ABM layer is scoped to *within one comisión* — two different comisiones of the same materia are allowed to share a time slot. Cross-comisión overlap **for a given student** is enforced separately, by `ValidadorSuperposicionHoraria` in the Inscripción a Cursada pipeline.
- `Alumno.usuarioId` is the `@Id` (via `@MapsId` to `Usuario`), not a field literally named `id`. Spring Data can't derive `findByAlumnoId...`-style queries against that — every repository that filters by alumno (`EstadoAcademicoMateriaRepository`, `InscripcionCursadaRepository`, `InscripcionFinalRepository`, `IntentoFinalRepository`, `CertificadoEmitidoRepository`) uses an explicit `@Query` navigating `alumno.usuarioId` instead. Follow the same pattern for any new alumno-scoped repository method.
- Inscripción a Cursada: `ValidadorInscripcionCursada` implementations (`@Order`ed, injected as `List<X>`) — correlatividad, superposición horaria, cupo, ventana de fechas. Enrolling auto-creates the `EstadoAcademicoMateria` row as `CURSANDO` if none exists (`EstadoAcademicoServiceImpl.marcarCursando`); it never regresses an already-more-advanced state.
- Inscripción a Final: `ValidadorInscripcionFinal` implementations — regularidad, superposición de mesas, límite de intentos, ventana de antelación (48h configurable). `IntentoFinal` rows are created on enrollment and removed on baja (docs/07 §2).
- `PlanMateria` uses `@IdClass(PlanMateriaId.class)` with both `plan` and `materia` as `@Id @ManyToOne` fields — `PlanMateriaId`'s field names/types must mirror those exactly. `MateriaRepository.buscar(carreraId, planId, anio)` does a `LEFT JOIN` through `PlanMateria`→`PlanEstudio`→`Carrera` so materias with no plan association still show up when no filters are given.
- Certificados/Exportación never persist the generated file — only `CertificadoEmitido` (id, alumno, tipo, fecha, SHA-256 hash) is stored, per docs/06 §10. `CertificadoFactory` (OpenPDF, `com.lowagie.text.*`) and `ExcelReportBuilder` (Apache POI) are the Factory/Builder patterns docs/01 §9 names.
- **OpenPDF version pin is load-bearing**: `openpdf` 2.2.5+ ships class files compiled for JDK 21 (class file version 65) and this project targets JDK 17 — javac rejects those class files ("bad class file... wrong version"), and because that error happens while compiling `CertificadoFactory`, it cascades into spurious "cannot find symbol getX()/builder()" errors across *every unrelated entity* in the same compilation (looks exactly like a broken Lombok annotation processor, but isn't). Pinned to `openpdf:2.0.5`, which is JDK 17-compatible. If bumping this dependency, verify `mvnw compile` still succeeds before assuming a Lombok regression.

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

All endpoints under `/api/v1`. Every error follows `ErrorResponse{codigo, mensaje, timestamp, detalles}` via `GlobalExceptionHandler`. Actual codes in use: `VALIDACION` (400), `CREDENCIALES_INVALIDAS`/`NO_AUTENTICADO` (401), `ACCESO_DENEGADO` (403), `RECURSO_NO_ENCONTRADO` (404), `*_DUPLICADO`/`*_DUPLICADA` (409 — `DNI_DUPLICADO`, `EMAIL_DUPLICADO`, `CARRERA_DUPLICADA`, `MATERIA_DUPLICADA`, `CORRELATIVIDAD_DUPLICADA`, `COMISION_DUPLICADA`, `MESA_DUPLICADA`, `PLAN_ESTUDIO_DUPLICADO`, `MATERIA_YA_ASOCIADA_AL_PLAN`, `INSCRIPCION_DUPLICADA`), business-rule violations at 422 (`ROL_INVALIDO`, `CORRELATIVIDAD_AUTOREFERENCIA`, `CORRELATIVIDAD_CICLICA`, `HORARIO_INVALIDO`, `HORARIO_SUPERPUESTO`, `MESA_FUERA_DE_FECHA`, `CONFIGURACION_FECHAS_INVALIDAS`, `REGLA_NEGOCIO_CORRELATIVIDAD`, `SUPERPOSICION_HORARIA`, `CUPO_AGOTADO`, `FUERA_DE_PLAZO`, `MATERIA_NO_REGULARIZADA`, `SUPERPOSICION_MESA`, `LIMITE_INTENTOS_SUPERADO`), `ERROR_INTERNO` (500, logged server-side — `GlobalExceptionHandler`'s catch-all must always log before returning the generic body, or failures become undiagnosable). Audit-only action names (not `ErrorResponse` codes, just `Auditoria.accion` values) follow the same all-caps convention — e.g. `CERTIFICADO_DESCARGADO`, `EXPORTACION_EXCEL`, `PLAN_ESTUDIO_CREADO`, `MATERIA_ASOCIADA_A_PLAN`, `INSCRIPCION_CURSADA_ALTA/MODIFICADA/BAJA`, `INSCRIPCION_FINAL_ALTA/MODIFICADA/BAJA`.

## Definition of Done

Every change should include: code in the correct layer (no logic in controllers), Bean Validation on request DTOs + explicit business validation in the service for anything Bean Validation can't express, errors mapped to the conventions above, a unit test per new business rule plus a MockMvc `*IT` test for role enforcement, an audit event for anything sensitive, and — since this has bitten before — an actual `mvnw verify` run plus a live `spring-boot:run` + curl/PowerShell check before calling something done. Tests passing has not been a reliable signal by itself in this project; several real bugs (data.sql load-order, H2-vs-MySQL SQL dialect differences, the audit transaction-phase bug, `Map.of()` NPEs on null-ish test fixtures) only showed up when actually running the app.
