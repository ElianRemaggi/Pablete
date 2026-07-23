# 10. Roadmap y Entregables

## 1. Etapas de desarrollo

| Etapa | Nombre | Contenido | Estado |
|---|---|---|---|
| 1 | Análisis y arquitectura | Documentos 01-09 de esta entrega: contexto, arquitectura, modelo de datos, entidades/DTO, API, frontend, seguridad, reglas de negocio, auditoría, diagramas | **Completada** |
| 2 | Setup de proyecto | Scaffolding backend (Maven, estructura de paquetes) | **Completada (backend)**. Scaffolding frontend, docker-compose y CI: pendientes |
| 3 | Backend — dominio base | Entidades JPA, migraciones (Flyway) a partir del DDL, repositories, seguridad JWT completa, autenticación funcional | **Completada** |
| 4 | Backend — módulos académicos | Carreras, materias, correlatividades, comisiones, horarios, mesas de examen, planes de estudio (`plan_estudio`/`plan_materia`) (CRUD + validaciones) | **Completada** |
| 4.5 | Backend — configuración institucional y auditoría | Adelantado desde la Etapa 5: ConfiguracionInstitucional (fechas, límite de intentos, bloqueo) y auditoría por eventos (`docs/08`) wireada en todos los servicios existentes | **Completada** |
| 5 | Backend — inscripciones, historial, certificados y exportación | Inscripción a cursadas y finales con el pipeline completo de validadores; alumnos + historial académico; generación de certificados PDF (OpenPDF); exportación a Excel (Apache POI) | **Completada** |
| 6 | Frontend — dashboards | Login, dashboard Alumno, Preceptor, Administrador, todos los módulos CRUD y de inscripción conectados a la API real | Pendiente |
| 7 | Endurecimiento | Rate limiting, revisión de seguridad final, tests E2E de flujos críticos, documentación Swagger final | Pendiente (hay tests unitarios/integración por módulo desde la Etapa 3, falta la pasada final) |
| 8 | Despliegue | Contenerización final, pipeline CI/CD, entorno de staging, capacitación a personal administrativo, migración de datos históricos desde Excel | Pendiente |

Ver `../CLAUDE.md` (raíz del repo) para el detalle técnico exacto de lo implementado: comandos de build/test, paquetes reales, y decisiones/desvíos puntuales respecto a este documento que surgieron al implementar (por ejemplo, `Comision`/`MesaExamen` no tienen baja lógica porque el DDL no les da columna `activa`).

## 2. Backlog técnico priorizado (alto nivel)

**Must have (MVP):**
- ✅ Autenticación JWT (login, refresh, logout).
- ✅ ABM de usuarios y roles.
- ✅ ABM de oferta académica (carrera, materia, correlatividad, comisión, horario, mesa de examen, plan de estudio).
- ✅ Inscripción y baja a cursadas con validaciones completas.
- ✅ Inscripción y baja a finales con validaciones completas.
- ✅ Historial académico del alumno (`/alumnos/me/historial-academico`, materias aprobadas/pendientes).
- ✅ Configuración institucional (fechas, límite de intentos, bloqueo de finales).
- ✅ Auditoría de acciones críticas.

**Should have:**
- ✅ Exportación a Excel (inscriptos por comisión/mesa, listado general de alumnos).
- ✅ Generación de certificados PDF (alumno regular, analítico parcial).
- Reportes con gráficos (ocupación de cupos, tasa de aprobación) — pendiente.

**Could have:**
- Notificaciones por email ante cambios de estado de inscripción o proximidad de cierre de fechas.
- Panel de auditoría con exportación de eventos.
- Modo oscuro / personalización de tema institucional.

**Won't have (en esta versión):**
- Aplicación móvil nativa (el frontend responsive cubre el uso desde mobile vía navegador).
- Integración con sistemas de pago o aranceles.

## 3. Criterios de aceptación por módulo (ejemplos representativos)

### Inscripción a cursada
- Dado un alumno sin la correlativa aprobada requerida, **cuando** intenta inscribirse a una comisión de una materia que la exige, **entonces** el sistema responde 422 con código `REGLA_NEGOCIO_CORRELATIVIDAD` y no persiste la inscripción.
- Dado un alumno ya inscripto en una comisión con horario Lunes 18-22, **cuando** intenta inscribirse a otra comisión con horario Lunes 19-21, **entonces** el sistema rechaza por superposición horaria.
- Dado que el cupo de una comisión está completo, **cuando** un alumno intenta inscribirse, **entonces** el sistema rechaza con código `CUPO_AGOTADO`.

### Inscripción a final
- Dado un alumno con 3 intentos ya registrados y `limiteIntentosFinal = 3`, **cuando** intenta inscribirse a una nueva mesa de la misma materia, **entonces** el sistema rechaza con `LIMITE_INTENTOS_SUPERADO`.
- Dado que faltan menos de 48 horas para una mesa, **cuando** un alumno intenta inscribirse, modificar o dar de baja su inscripción, **entonces** el sistema rechaza con `FUERA_DE_PLAZO`.

### Seguridad
- Dado un usuario con rol ALUMNO, **cuando** intenta acceder a un endpoint de administración (`/api/v1/usuarios`), **entonces** el sistema responde 403.
- Dado un access token expirado, **cuando** el cliente reintenta con el refresh token válido, **entonces** el sistema emite un nuevo access token sin requerir nuevo login.

### Auditoría
- Dado cualquier alta/baja/modificación sobre usuarios, inscripciones o configuración institucional, **cuando** la operación se confirma, **entonces** se genera un registro en `auditoria` con usuario, IP, acción, entidad y detalle correspondiente, consultable por el Administrador.

## 4. Definition of Done (por historia de usuario)

- Código en la capa correcta según Clean Architecture (sin lógica de negocio en Controllers).
- Validaciones de entrada (Bean Validation) y de negocio (validadores dedicados) implementadas.
- Manejo de errores mapeado al catálogo de `04-api-endpoints.md` (§15).
- Tests unitarios de servicio (reglas de negocio) y tests de integración del endpoint (MockMvc / Testcontainers con MySQL).
- Evento de auditoría publicado si la operación es sensible.
- Endpoint documentado en Swagger/OpenAPI.
- Revisión de código (pull request) aprobada antes de merge a `develop`.

## 5. Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Migración de datos históricos desde Excel con inconsistencias | Etapa dedicada de limpieza/mapeo de datos antes de la migración final, con scripts de validación previos a la carga |
| Adopción por parte del personal administrativo acostumbrado a Excel | Capacitación específica y período de convivencia (Excel + sistema) antes del corte definitivo |
| Picos de carga en períodos de inscripción (todos los alumnos inscribiéndose el mismo día) | Índices ya contemplados en el modelo de datos, diseño stateless que permite escalar horizontalmente el backend, rate limiting para evitar abuso |
| Cambios de reglamento académico a mitad de ciclo lectivo | Todas las reglas configurables (fechas, intentos, bloqueo) están externalizadas en `configuracion_institucional`, no hardcodeadas |

## 6. Cierre de esta entrega

Esta documentación cubre de forma completa la **Etapa 1: Análisis y Arquitectura**, incluyendo modelo de datos, entidades, contrato de API, frontend, seguridad, reglas de negocio, auditoría y diagramas. Sirve como base de diseño para iniciar directamente la implementación del código fuente en las etapas siguientes, sin ambigüedades pendientes de definición funcional.
