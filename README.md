# SIGA — Sistema Web de Gestión Académica

Reemplaza el proceso manual de inscripciones por Google Forms + Excel de un instituto educativo: autenticación por roles, ABM de oferta académica, inscripción a cursadas/finales con validación automática de reglas de negocio, y auditoría completa de acciones sensibles.

## Estado actual

Backend en desarrollo activo sobre Spring Boot. Implementado y con tests (unitarios + integración):

- Autenticación JWT (login por DNI, refresh, logout)
- ABM de usuarios y roles
- CRUD de Carrera, Materia, Correlatividad (con detección de ciclos), Comisión/Horario (con detección de superposición horaria), Mesa de Examen
- Configuración institucional (ventanas de inscripción, límite de intentos)
- Auditoría de toda acción sensible (evento genérico + listener asíncrono)

Pendiente: Plan de Estudio, el pipeline de inscripción a cursadas/finales, certificados PDF, exportación a Excel, frontend, y despliegue. Ver el detalle etapa por etapa en [`docs/10-roadmap-entregables.md`](docs/10-roadmap-entregables.md).

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

## Stack

Java 17, Spring Boot 3, Spring Security 6 (JWT), Spring Data JPA, MapStruct, Maven, H2 (dev) / MySQL 8 (prod) + Flyway.
