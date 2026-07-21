---
name: siga-scaffold-module
description: >
  Scaffold a new SIGA backend CRUD module (entity, request/response DTOs,
  MapStruct mapper, repository, service interface + impl, controller) that
  follows this repo's Clean Architecture conventions exactly. Use when adding
  a new resource under com.instituto.siga — Carrera, PlanEstudio, Materia,
  Correlatividad, Comision, Horario, MesaExamen, ConfiguracionInstitucional,
  or a similar academic-offering entity — or when the user asks to "scaffold
  a module", "add a CRUD for X", "create the entity/DTO/service/controller
  for X". Do NOT use for the enrollment flows (inscripcion_cursada /
  inscripcion_final) — those need the validator pipeline, see
  siga-add-validator instead.
license: MIT
---

# SIGA — scaffold a backend module

Before writing anything, read the three docs that define the exact contract
for the entity in question — don't invent field names, endpoints, or rules:

- `docs/02-modelo-datos.md` — table, columns, constraints, indexes for this entity.
- `docs/03-entidades-dto.md` — JPA entity shape, request/response DTOs, mapper.
- `docs/04-api-endpoints.md` — exact routes, HTTP methods, required role per route.

## Layout (one file per layer, matching `docs/01-analisis-arquitectura.md` §5.1)

```
entity/<Entidad>.java                          # JPA entity, matches the DDL column-for-column
dto/request/<Entidad>Request.java              # Bean Validation annotations from the DDL constraints
dto/response/<Entidad>Response.java
mapper/<Entidad>Mapper.java                    # MapStruct interface, entity <-> DTO only
repository/<Entidad>Repository.java            # Spring Data JPA, no custom SQL unless a doc calls for it
service/interfaces/<Entidad>Service.java       # Port — controller depends on this, not the impl
service/impl/<Entidad>ServiceImpl.java         # All business logic and orchestration lives here
controller/<Entidad>Controller.java            # Thin: bind request -> call service -> map response + status
```

## Rules (non-negotiable, from CLAUDE.md / docs, not up for ponytail-style trimming)

- **Controller has zero business logic.** It receives the request, calls exactly one service method via the interface, returns the response DTO with the HTTP status from `docs/04`. If a rule wants to live in the controller, it belongs in the service instead.
- **Role/route match the catalog exactly.** Copy the role restriction (`hasRole('ADMINISTRADOR')` etc.) straight from the `docs/04` table for this resource — don't guess broader or narrower access.
- **Constraints from the DDL become Bean Validation.** `NOT NULL` → `@NotNull`/`@NotBlank`, `UNIQUE` → checked in the service with a `BusinessException` (not just left to the DB constraint to throw an opaque 500), `CHECK` clauses → matching `@AssertTrue`/custom validator or explicit service check.
- **Soft delete, not hard delete**, wherever the docs describe "baja lógica" (e.g. `carrera.activa`, `materia.activa`) — a DELETE endpoint flips a flag, it doesn't remove the row.
- **Audit sensitive mutations.** Per `docs/08-auditoria.md`, alta/baja/modificación of academic-offering entities and institutional config must publish a domain event (`ApplicationEventPublisher`) rather than call the audit service directly from the business service. If this module is in the table in `docs/08` §2, wire the event.
- **Errors map to the catalog in `docs/04` §15** — a rule violation is a `BusinessException` with the specific error code, returned as 422 via the existing `GlobalExceptionHandler`, never a raw stack trace or a generic 400.
- **No new module-specific abstractions** beyond this shape — one repository, one service interface, one impl, one controller. If the module needs a second implementation of the service interface or a factory, that's a sign the docs called for it explicitly; if not, don't add it.

## After scaffolding

- Add/verify the Swagger annotation on the controller so the endpoint shows up in OpenAPI (part of Definition of Done in `docs/10-roadmap-entregables.md` §4).
- Write the service-level unit test for any business rule this module enforces, plus a MockMvc/Testcontainers integration test for the endpoint — same DoD section.
