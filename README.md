# SIGA — Sistema Web de Gestión Académica

Reemplaza el proceso manual de inscripciones por Google Forms + Excel de un instituto educativo: autenticación por roles, ABM de oferta académica, inscripción a cursadas/finales con validación automática de reglas de negocio, y auditoría completa de acciones sensibles.

## Estado actual

Backend en desarrollo activo sobre Spring Boot. Implementado y con tests (unitarios + integración):

- Autenticación JWT (login por DNI, refresh, logout)
- ABM de usuarios y roles
- CRUD de Carrera, Materia (+ Plan de Estudio y asociación Materia-Plan), Correlatividad (con detección de ciclos), Comisión/Horario (con detección de superposición horaria), Mesa de Examen
- Inscripción a cursadas y a finales, con el pipeline completo de validaciones de negocio (correlatividad, superposición horaria, cupo, regularidad, límite de intentos, ventanas de fecha)
- Historial académico del alumno (materias aprobadas/pendientes)
- Certificados PDF (alumno regular, analítico parcial) y exportación a Excel (inscriptos por comisión/mesa, listado de alumnos)
- Configuración institucional (ventanas de inscripción, límite de intentos)
- Auditoría de toda acción sensible (evento genérico + listener asíncrono)

Con esto el backend cubre el MVP completo ("Must have"/"Should have") de `docs/10-roadmap-entregables.md` §2.

El frontend (`frontend/`, Vite + React + TypeScript + MUI) también está implementado: login, layouts por rol, y todos los módulos CRUD/de inscripción conectados a la API real (ABM de Administrador, búsqueda/ficha/exportaciones de Preceptor, dashboard/historial/inscripciones/certificados de Alumno). Pendiente: despliegue, rate limiting y reportes con gráficos más allá de ocupación de cupos. Ver el detalle etapa por etapa en [`docs/10-roadmap-entregables.md`](docs/10-roadmap-entregables.md).

## Documentación

- [`docs/00-README.md`](docs/00-README.md) — índice y orden de lectura de la documentación de diseño completa (arquitectura, modelo de datos, API, seguridad, reglas de negocio, auditoría).
- [`CLAUDE.md`](CLAUDE.md) — estado técnico exacto de la implementación: comandos, paquetes reales, decisiones y desvíos puntuales respecto al diseño original que surgieron al construirlo.

## Backend — cómo correrlo

Requiere **JDK 17+** (Maven no hace falta instalarlo, el repo incluye el wrapper).

```bash
cd backend
./mvnw test              # tests unitarios
./mvnw verify             # tests unitarios + de integración
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Por defecto corre con perfil `dev`: base H2 en memoria, sembrada con 3 usuarios de prueba (login por DNI):

| DNI | Contraseña | Rol |
|---|---|---|
| `30111222` | `Admin1234` | ADMINISTRADOR |
| `30222333` | `Preceptor1234` | PRECEPTOR |
| `30333444` | `Alumno1234` | ALUMNO |

Documentación interactiva de la API en `/swagger-ui.html`.

Para producción (perfil `prod`) usa MySQL 8 + migraciones Flyway; ver `backend/src/main/resources/application-prod.yml`.

## Frontend — cómo correrlo

Requiere Node 18+.

```bash
cd frontend
npm install
npm run dev   # sirve en http://localhost:5173 por defecto
```

Si el backend no corre en `localhost:8080`, creá `frontend/.env.development.local` (gitignored) con:

```
VITE_API_BASE_URL=http://localhost:<puerto>/api/v1
```

## Stack

**Backend**: Java 17, Spring Boot 3, Spring Security 6 (JWT), Spring Data JPA, MapStruct, Maven, H2 (dev) / MySQL 8 (prod) + Flyway, OpenPDF (certificados), Apache POI (exportación Excel).

**Frontend**: Vite, React 19 + TypeScript, Material UI, React Router, React Hook Form + Zod, Axios, TanStack Query, recharts.
