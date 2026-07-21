# 01. Análisis del Sistema y Arquitectura

## 1. Contexto y problema actual

El instituto gestiona hoy dos procesos críticos de forma manual y desacoplada:

- **Inscripción a cursadas** vía Google Forms.
- **Inscripción a finales** vía Google Forms.
- **Consolidación de datos** vía copiado manual a planillas Excel por personal administrativo.

Esto genera: errores humanos, alto costo de tiempo administrativo, datos duplicados o inconsistentes, ausencia de validaciones de negocio (correlatividades, cupos, superposición horaria), trazabilidad nula de cambios y riesgo real de pérdida de información (archivos de Excel sin versionado ni backup confiable).

## 2. Objetivo del sistema

Reemplazar completamente Google Forms + Excel por un **Sistema Web de Gestión Académica (SIGA)** que automatice de punta a punta:

- Autenticación y gestión de usuarios por rol.
- Inscripción a cursadas y a finales, con validación automática de reglas de negocio.
- Consulta de historial académico, materias aprobadas/pendientes y estado académico.
- Emisión de certificados en PDF y exportación de información a Excel.
- Administración completa de la oferta académica (carreras, planes, materias, correlatividades, horarios, comisiones, mesas de examen) y de las reglas institucionales (fechas, intentos, bloqueos).
- Auditoría de toda acción sensible.

## 3. Alcance funcional (resumen)

| Actor | Puede hacer |
|---|---|
| **Alumno** | Login DNI/contraseña, ver datos personales, historial académico, materias aprobadas/pendientes, inscribirse/modificar inscripción a cursadas y finales, descargar certificado de alumno regular y analítico parcial (PDF) |
| **Preceptor** | Buscar alumnos, consultar estado académico, ver inscripciones, modificar información permitida, exportar a Excel, descargar certificados, supervisar alumnos |
| **Administrador** | ABM de usuarios/roles, gestión completa de carreras, planes de estudio, materias, correlatividades, horarios, comisiones, mesas de examen, reglas institucionales, fechas de inscripción, límite de intentos, bloqueo de finales |

El detalle de reglas de negocio está en `07-reglas-negocio-validaciones.md`.

## 4. Requerimientos no funcionales

- **Seguridad**: JWT + Spring Security, hashing BCrypt, control de acceso por rol y por recurso.
- **Escalabilidad**: arquitectura en capas desacoplada, stateless en backend (JWT), posibilidad de escalar horizontalmente detrás de un balanceador.
- **Mantenibilidad**: Clean Architecture, SOLID, capas bien separadas, cobertura de tests.
- **Disponibilidad**: contenerización con Docker, health-checks, migraciones versionadas.
- **Protección de datos**: cifrado de contraseñas, sanitización de entradas, principio de mínimo privilegio en roles, auditoría de accesos.
- **Responsive**: frontend adaptable a mobile/tablet/desktop (Material UI + Grid responsive).

## 5. Decisión arquitectónica: Clean Architecture

Se adopta **Clean Architecture** con separación estricta en capas concéntricas. La regla de dependencia es unidireccional: las capas externas dependen de las internas, nunca al revés.

```
┌──────────────────────────────────────────────────────────┐
│                     Frameworks & Drivers                  │
│   Controllers · Security Config · Persistence (JPA) ·     │
│   External (PDF/Excel libs) · Web (React)                 │
│  ┌────────────────────────────────────────────────────┐  │
│  │              Interface Adapters                     │  │
│  │   DTO · Mapper · Repository (impl) · Presenters     │  │
│  │  ┌────────────────────────────────────────────────┐│  │
│  │  │            Application / Use Cases              ││  │
│  │  │   Services (reglas de aplicación, orquestación)  ││  │
│  │  │  ┌──────────────────────────────────────────┐  ││  │
│  │  │  │             Domain / Entities             │  ││  │
│  │  │  │  Entidades, Value Objects, reglas puras   │  ││  │
│  │  │  └──────────────────────────────────────────┘  ││  │
│  │  └────────────────────────────────────────────────┘│  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

### 5.1 Paquetes del backend (Java / Spring Boot)

```
com.instituto.siga
├── config/                # Beans, CORS, OpenAPI/Swagger, Jackson, Async
├── security/
│   ├── jwt/                # JwtProvider, JwtFilter, JwtAuthEntryPoint
│   ├── config/              # SecurityConfig, PasswordEncoderConfig
│   └── userdetails/          # CustomUserDetailsService
├── controller/
│   ├── AuthController
│   ├── UsuarioController
│   ├── AlumnoController
│   ├── PreceptorController
│   ├── CarreraController
│   ├── PlanEstudioController
│   ├── MateriaController
│   ├── CorrelatividadController
│   ├── ComisionController
│   ├── HorarioController
│   ├── InscripcionCursadaController
│   ├── MesaExamenController
│   ├── InscripcionFinalController
│   ├── HistorialAcademicoController
│   ├── CertificadoController
│   ├── ExportacionController
│   ├── ConfiguracionInstitucionalController
│   └── AuditoriaController
├── service/
│   ├── interfaces/           # Contratos (puertos) de cada caso de uso
│   └── impl/                 # Implementaciones con toda la lógica de negocio
├── repository/               # Spring Data JPA repositories
├── dto/
│   ├── request/
│   └── response/
├── mapper/                   # MapStruct mappers Entity <-> DTO
├── entity/                   # Entidades JPA (ver 03-entidades-dto.md)
├── exception/
│   ├── GlobalExceptionHandler
│   ├── BusinessException
│   ├── ResourceNotFoundException
│   └── ValidationException
├── validation/                # Validadores custom (@Correlatividad, @HorarioValido, etc.)
├── util/                      # Constantes, utilitarios de fecha, generación PDF/Excel helpers
└── audit/                     # Interceptores/aspectos de auditoría
```

**Regla estricta:** los `Controller` sólo reciben el request, delegan al `Service` correspondiente vía su interfaz, y devuelven el DTO de respuesta con el código HTTP adecuado. Ninguna regla de negocio, validación de dominio ni acceso a repositorio ocurre en el controller.

### 5.2 Estructura del frontend (React + TypeScript)

```
src/
├── app/                       # Configuración global: store, router, theme
├── api/                       # Cliente Axios, interceptores (JWT, refresh)
├── auth/                      # Contexto de autenticación, hooks (useAuth)
├── modules/
│   ├── login/
│   ├── alumno/
│   │   ├── dashboard/
│   │   ├── inscripcion-cursada/
│   │   ├── inscripcion-final/
│   │   ├── historial-academico/
│   │   └── certificados/
│   ├── preceptor/
│   │   ├── busqueda-alumnos/
│   │   ├── estado-academico/
│   │   └── exportacion/
│   └── administrador/
│       ├── usuarios/
│       ├── carreras/
│       ├── planes-estudio/
│       ├── materias/
│       ├── correlatividades/
│       ├── horarios/
│       ├── comisiones/
│       ├── mesas-examen/
│       ├── configuracion-institucional/
│       └── auditoria/
├── components/                 # Componentes compartidos (Navbar, Sidebar, Table, Modal, Alert)
├── hooks/
├── types/
├── utils/
└── theme/                      # Tema institucional Material UI
```

## 6. Stack tecnológico confirmado

**Frontend:** React 18, TypeScript, Vite, Material UI (MUI), React Router, Axios, React Hook Form + Yup/Zod para validación de formularios.

**Backend:** Java 17+, Spring Boot 3.x, Spring Security 6, JWT (jjwt), Hibernate/JPA, Maven.

**Base de datos:** MySQL 8.

**Otros:** Lombok, MapStruct, OpenPDF (o iText) para generación de PDF, Apache POI para exportación Excel, Docker + docker-compose, Git con convención de branching (GitFlow simplificado: `main`, `develop`, `feature/*`, `release/*`, `hotfix/*`).

## 7. Vista de despliegue (Docker)

```
docker-compose.yml
├── mysql          (MySQL 8, volumen persistente, healthcheck)
├── backend        (Spring Boot, expone :8080, depende de mysql)
├── frontend       (Nginx sirviendo build de Vite, expone :80/:443)
└── (opcional) redis  (para rate limiting / cache de tokens revocados)
```

Variables de entorno externalizadas (`application.yml` + `.env`): credenciales de DB, secreto JWT, tiempos de expiración de access/refresh token, orígenes CORS permitidos.

## 8. Principios SOLID aplicados

- **S**: cada Service tiene una única responsabilidad (`InscripcionCursadaService` no conoce PDF ni Excel).
- **O**: reglas de validación implementadas como estrategias intercambiables (`ValidadorInscripcionCursada` compuesto por validadores atómicos: correlatividad, superposición horaria, cupo, fecha).
- **L**: toda implementación de `Repository`/`Service` respeta el contrato de su interfaz sin romper invariantes.
- **I**: interfaces de servicio segregadas por caso de uso (no una interfaz gigante `IAcademicoService`).
- **D**: los controllers dependen de interfaces de servicio, no de implementaciones concretas (inyección por constructor).

## 9. Patrones de diseño utilizados

- **Strategy**: validadores de inscripción combinables.
- **Factory**: generación de certificados PDF según tipo (`CertificadoFactory`).
- **Builder**: construcción de reportes Excel complejos (`ExcelReportBuilder`).
- **Facade**: `InscripcionFacadeService` orquesta validación + persistencia + auditoría en una transacción.
- **Observer/Event-driven**: `ApplicationEventPublisher` de Spring para disparar auditoría tras cada acción sensible sin acoplar el Service al módulo de auditoría.
- **DTO + Mapper (MapStruct)**: desacople entre entidad de persistencia y contrato expuesto por la API.
- **Repository**: abstracción de acceso a datos vía Spring Data JPA.
- **Chain of Responsibility**: pipeline de validaciones de inscripción a finales (regularidad → superposición → intentos → antelación 48h).

## 10. Siguiente etapa

Con el análisis y la arquitectura definidos, la siguiente etapa es el **modelado completo de la base de datos** (documento `02-modelo-datos.md`), seguida de entidades/DTOs, API REST, frontend, seguridad, reglas de negocio, auditoría, diagramas UML y roadmap de entrega.
