# SIGA — Sistema Web de Gestión Académica

Reemplaza el proceso manual de inscripciones por Google Forms + Excel de un instituto educativo: autenticación por roles, ABM de oferta académica, inscripción a cursadas/finales con validación automática de reglas de negocio, y auditoría completa de acciones sensibles.

## 🚀 Probarlo rápido con Docker (recomendado)

Esta es la forma más simple de levantar todo el sistema (backend + frontend) sin instalar Java, Node ni ninguna base de datos a mano. Solo necesitás **Docker Desktop**.

### 1. Instalar Docker Desktop (si no lo tenés)

Descargalo de [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/), instalalo, y abrilo. Esperá a que el ícono de Docker (una ballena, abajo a la derecha en Windows) diga que está corriendo — puede tardar uno o dos minutos la primera vez.

### 2. Levantar todo

Abrí una terminal (PowerShell o similar) en la carpeta raíz de este proyecto (donde está este mismo archivo `README.md`) y corré:

```bash
docker compose up --build
```

La primera vez tarda varios minutos (descarga imágenes y compila todo). Vas a ver mucho texto pasando — es normal. Cuando se calme y veas líneas como `Started SigaApplication` (del backend), ya está todo arriba.

**No cierres esa terminal** — mientras esté abierta, el sistema sigue corriendo. Para apagar todo, apretá `Ctrl+C` en esa misma terminal, o abrí otra y corré `docker compose down`.

### 3. Abrir el sistema

Andá a **http://localhost:8081** en el navegador. Iniciá sesión con alguno de estos 3 usuarios de prueba (se cargan solos, no hay que crear nada):

| DNI | Contraseña | Rol |
|---|---|---|
| `30111222` | `Admin1234` | ADMINISTRADOR |
| `30222333` | `Preceptor1234` | PRECEPTOR |
| `30333444` | `Alumno1234` | ALUMNO |

Eso es todo. Los datos se guardan en memoria mientras el contenedor está corriendo — si lo apagás con `docker compose down` y lo volvés a levantar, arranca de nuevo desde cero con estos mismos 3 usuarios (es intencional: este modo es para probar el sistema, no para guardar datos reales de forma permanente).

### Si algo no funciona

- **El login falla apenas abrís la página** → esperá 10-15 segundos más. El backend (Spring Boot) puede tardar un poco en terminar de arrancar aunque el contenedor ya figure como iniciado, y probá de nuevo.
- **"docker: command not found" o similar** → Docker Desktop no está instalado o no está corriendo. Abrilo y esperá a que diga "Running".
- **Puerto ocupado (`port is already allocated`)** → algo más en tu máquina está usando el puerto 8080 u 8081. Cerrá ese programa, o avisá para cambiar los puertos en `docker-compose.yml`.
- **Quiero volver a compilar de cero** → `docker compose down` y después `docker compose up --build` de nuevo.
- **Quiero ver los logs de un solo servicio** → `docker compose logs -f backend` (o `frontend`).

## Documentación

- [`docs/00-README.md`](docs/00-README.md) — índice y orden de lectura de la documentación de diseño completa (arquitectura, modelo de datos, API, seguridad, reglas de negocio, auditoría).
- [`CLAUDE.md`](CLAUDE.md) — estado técnico exacto de la implementación: comandos, paquetes reales, decisiones y desvíos puntuales respecto al diseño original que surgieron al construirlo.

## Estado actual

Backend y frontend completos, cubriendo el MVP entero ("Must have"/"Should have") de [`docs/10-roadmap-entregables.md`](docs/10-roadmap-entregables.md) §2:

- Autenticación JWT (login por DNI, refresh, logout) y ABM de usuarios/roles
- CRUD completo de oferta académica: Carrera, Plan de Estudio, Materia, Correlatividad (detección de ciclos), Comisión/Horario (detección de superposición), Mesa de Examen
- Inscripción a cursadas y a finales con el pipeline completo de validaciones de negocio
- Historial académico, certificados PDF, exportación a Excel, configuración institucional, auditoría
- Frontend (React + MUI) con dashboards y ABMs para los 3 roles (Alumno, Preceptor, Administrador), incluyendo reportes con gráficos

Pendiente: CI/CD, entorno de staging con base de datos persistente, rate limiting, y notificaciones por email (backlog de baja prioridad). Ver detalle etapa por etapa en `docs/10-roadmap-entregables.md`.

## Alternativa: correr sin Docker (para desarrollo)

Si vas a modificar código, es más cómodo correr cada parte directo en tu máquina (hot-reload, debugging, etc.) en vez de reconstruir la imagen de Docker cada vez.

### Backend

Requiere **JDK 17+** (Maven no hace falta instalarlo, el repo incluye el wrapper).

```bash
cd backend
./mvnw test              # tests unitarios
./mvnw verify             # tests unitarios + de integración
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8080
```

Por defecto corre con perfil `dev`: base H2 en memoria, sembrada con los mismos 3 usuarios de prueba de la tabla de arriba (login por DNI).

Documentación interactiva de la API en `/swagger-ui.html`.

Para producción (perfil `prod`) usa MySQL 8 + migraciones Flyway; ver `backend/src/main/resources/application-prod.yml`.

### Frontend

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

**Infraestructura**: Docker + Docker Compose (`docker-compose.yml` en la raíz) para levantar todo con un solo comando.
