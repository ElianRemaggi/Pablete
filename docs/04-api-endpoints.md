# 04. API REST — Catálogo de Endpoints

Convención general: prefijo `/api/v1`. Respuestas de error usan `ErrorResponse` (ver `03-entidades-dto.md`) con status HTTP semántico (400, 401, 403, 404, 409, 422).

## 1. Autenticación — `/api/v1/auth`

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| POST | `/auth/login` | Público | Login con DNI + contraseña, devuelve access + refresh token |
| POST | `/auth/refresh` | Público (con refresh token) | Renueva access token |
| POST | `/auth/logout` | Autenticado | Revoca refresh token actual |

## 2. Usuarios — `/api/v1/usuarios`

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| GET | `/usuarios` | ADMIN | Listado paginado, filtros por rol/estado |
| GET | `/usuarios/{id}` | ADMIN | Detalle de usuario |
| POST | `/usuarios` | ADMIN | Alta de usuario (con rol asignado) |
| PUT | `/usuarios/{id}` | ADMIN | Modificación de datos |
| PATCH | `/usuarios/{id}/activar` | ADMIN | Activar usuario |
| PATCH | `/usuarios/{id}/desactivar` | ADMIN | Baja lógica |
| PUT | `/usuarios/{id}/roles` | ADMIN | Modificar roles asignados |
| GET | `/usuarios/me` | Autenticado | Datos del usuario logueado |

## 3. Alumnos — `/api/v1/alumnos`

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| GET | `/alumnos/me` | ALUMNO | Info personal del alumno logueado |
| GET | `/alumnos/{id}` | PRECEPTOR, ADMIN | Info de un alumno puntual |
| GET | `/alumnos` | PRECEPTOR, ADMIN | Búsqueda de alumnos (por legajo, DNI, apellido) |
| GET | `/alumnos/me/historial-academico` | ALUMNO | Historial académico propio |
| GET | `/alumnos/{id}/historial-academico` | PRECEPTOR, ADMIN | Historial académico de un alumno |
| GET | `/alumnos/me/materias-aprobadas` | ALUMNO | Listado de materias aprobadas |
| GET | `/alumnos/me/materias-pendientes` | ALUMNO | Listado de materias pendientes |

## 4. Carreras / Planes de estudio — `/api/v1/carreras`

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| GET | `/carreras` | Todos autenticados | Listado |
| POST | `/carreras` | ADMIN | Alta |
| PUT | `/carreras/{id}` | ADMIN | Modificación |
| DELETE | `/carreras/{id}` | ADMIN | Baja lógica |
| GET | `/carreras/{id}/planes` | Todos autenticados | Planes de estudio de la carrera |
| POST | `/carreras/{id}/planes` | ADMIN | Alta de plan de estudio |
| PUT | `/planes-estudio/{id}` | ADMIN | Modificación de plan |

## 5. Materias — `/api/v1/materias`

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| GET | `/materias` | Todos autenticados | Listado con filtros (carrera, plan, año) |
| GET | `/materias/{id}` | Todos autenticados | Detalle |
| POST | `/materias` | ADMIN | Alta |
| PUT | `/materias/{id}` | ADMIN | Modificación |
| DELETE | `/materias/{id}` | ADMIN | Baja lógica |
| POST | `/materias/{id}/plan/{planId}` | ADMIN | Asociar materia a plan (año/cuatrimestre) |

## 6. Correlatividades — `/api/v1/correlatividades`

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| GET | `/correlatividades/materia/{materiaId}` | Todos autenticados | Correlativas de una materia |
| POST | `/correlatividades` | ADMIN | Alta de correlatividad |
| DELETE | `/correlatividades/{id}` | ADMIN | Baja |

## 7. Comisiones y horarios — `/api/v1/comisiones`

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| GET | `/comisiones` | Todos autenticados | Listado (filtro por materia, período) |
| POST | `/comisiones` | ADMIN | Alta (incluye horarios) |
| PUT | `/comisiones/{id}` | ADMIN | Modificación |
| DELETE | `/comisiones/{id}` | ADMIN | Baja |
| POST | `/comisiones/{id}/horarios` | ADMIN | Agregar horario |
| DELETE | `/comisiones/{id}/horarios/{horarioId}` | ADMIN | Quitar horario |

## 8. Inscripción a cursadas — `/api/v1/inscripciones-cursada`

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| GET | `/inscripciones-cursada/me` | ALUMNO | Inscripciones propias vigentes |
| POST | `/inscripciones-cursada` | ALUMNO | Inscribirse a una comisión (valida correlatividad, superposición, cupo, fecha) |
| PUT | `/inscripciones-cursada/{id}` | ALUMNO | Modificar inscripción (antes del cierre) |
| DELETE | `/inscripciones-cursada/{id}` | ALUMNO | Dar de baja inscripción |
| GET | `/inscripciones-cursada/comision/{comisionId}` | PRECEPTOR, ADMIN | Listado de inscriptos por comisión |
| GET | `/inscripciones-cursada/alumno/{alumnoId}` | PRECEPTOR, ADMIN | Historial de inscripciones a cursada de un alumno puntual (agregado tras implementar el frontend — no estaba en el diseño original) |

## 9. Mesas de examen — `/api/v1/mesas-examen`

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| GET | `/mesas-examen` | Todos autenticados | Listado (filtro por materia, fecha) |
| POST | `/mesas-examen` | ADMIN | Alta |
| PUT | `/mesas-examen/{id}` | ADMIN | Modificación |
| DELETE | `/mesas-examen/{id}` | ADMIN | Baja |

## 10. Inscripción a finales — `/api/v1/inscripciones-final`

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| GET | `/inscripciones-final/me` | ALUMNO | Inscripciones propias |
| POST | `/inscripciones-final` | ALUMNO | Inscribirse a mesa (valida regularidad, superposición, intentos, antelación 48h) |
| PUT | `/inscripciones-final/{id}` | ALUMNO | Modificar (antes del cierre) |
| DELETE | `/inscripciones-final/{id}` | ALUMNO | Dar de baja |
| GET | `/inscripciones-final/mesa/{mesaId}` | PRECEPTOR, ADMIN | Listado de inscriptos por mesa |
| GET | `/inscripciones-final/alumno/{alumnoId}` | PRECEPTOR, ADMIN | Historial de inscripciones a final de un alumno puntual (agregado tras implementar el frontend — no estaba en el diseño original) |

## 11. Certificados — `/api/v1/certificados`

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| GET | `/certificados/alumno-regular` | ALUMNO | Genera y descarga PDF de alumno regular |
| GET | `/certificados/analitico-parcial` | ALUMNO | Genera y descarga PDF de analítico parcial |
| GET | `/certificados/alumno/{id}/alumno-regular` | PRECEPTOR, ADMIN | Emite certificado para un alumno puntual |
| GET | `/certificados/historial` | ALUMNO | Historial de certificados emitidos propios |
| GET | `/certificados/inscripcion-cursada/{id}` | ALUMNO | Comprobante PDF de una inscripción a cursada propia (agregado tras la actualización de identidad visual/calendario — no estaba en el diseño original) |
| GET | `/certificados/inscripcion-final/{id}` | ALUMNO | Comprobante PDF de una inscripción a final propia |
| GET | `/certificados/alumno/{alumnoId}/inscripcion-cursada/{id}` | PRECEPTOR, ADMIN | Comprobante de una inscripción a cursada de un alumno puntual |
| GET | `/certificados/alumno/{alumnoId}/inscripcion-final/{id}` | PRECEPTOR, ADMIN | Comprobante de una inscripción a final de un alumno puntual |

## 12. Exportación — `/api/v1/exportaciones`

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| GET | `/exportaciones/inscripciones-cursada?comisionId=` | PRECEPTOR, ADMIN | Excel de inscriptos a una comisión |
| GET | `/exportaciones/inscripciones-final?mesaId=` | PRECEPTOR, ADMIN | Excel de inscriptos a una mesa |
| GET | `/exportaciones/alumnos` | PRECEPTOR, ADMIN | Excel general de alumnos con filtros |

## 13. Configuración institucional — `/api/v1/configuracion`

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| GET | `/configuracion` | Todos autenticados | Consulta fechas/reglas vigentes |
| PUT | `/configuracion` | ADMIN | Modificar fechas de apertura/cierre, límite de intentos, bloqueo de finales |

## 14. Auditoría — `/api/v1/auditoria`

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| GET | `/auditoria` | ADMIN | Listado paginado con filtros (usuario, acción, entidad, rango de fechas) |
| GET | `/auditoria/{id}` | ADMIN | Detalle de un evento de auditoría |

## 15. Reportes — `/api/v1/reportes`

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| GET | `/reportes/tasa-aprobacion-por-materia` | PRECEPTOR, ADMIN | Aprobadas vs. total de `estado_academico_materia` por materia (agregado tras implementar el frontend — no estaba en el diseño original; ocupación de cupos e inscriptos por comisión, mencionados en `docs/05` §6, se resuelven en el frontend combinando `GET /comisiones` con `GET /inscripciones-cursada/comision/{id}`, sin necesitar un endpoint propio) |

## 16. Calendario y Eventos Académicos — `/api/v1/calendario`, `/api/v1/eventos-academicos`

Agregado en la actualización de identidad visual/calendario (no estaba en el diseño original). Los eventos de tipo cursada, final e inscripción **no se persisten en una tabla propia**: se derivan en tiempo real de `horario`+`comision` (expandiendo la recurrencia semanal a fechas concretas), `mesa_examen`, `inscripcion_cursada`/`inscripcion_final` y `configuracion_institucional`. Solo `evento_academico` (eventos institucionales sueltos y fechas límite cargadas a mano) es una tabla nueva.

| Método | Endpoint | Rol | Descripción |
|---|---|---|---|
| GET | `/calendario/me?desde=&hasta=` | ALUMNO | Calendario propio: sus cursadas, finales, inscripciones y fechas límite en el rango |
| GET | `/calendario/alumno/{alumnoId}?desde=&hasta=` | PRECEPTOR, ADMIN | Calendario de un alumno puntual (usado por la ficha de alumno) |
| GET | `/calendario/institucional?desde=&hasta=` | PRECEPTOR, ADMIN | Todas las cursadas, mesas, ventanas y eventos académicos del rango |
| GET | `/eventos-academicos` | Todos autenticados | Listado de eventos institucionales cargados a mano |
| POST | `/eventos-academicos` | ADMIN | Alta de evento académico o fecha límite |
| PUT | `/eventos-academicos/{id}` | ADMIN | Modificación |
| DELETE | `/eventos-academicos/{id}` | ADMIN | Baja (física — no hay noción de "activo" para estos eventos) |

`desde`/`hasta` son fechas (`yyyy-MM-dd`); si se omiten, el servicio usa el mes calendario actual por defecto.

## 17. Convenciones de error

| Código HTTP | Uso |
|---|---|
| 400 | Datos de entrada inválidos (bean validation) |
| 401 | No autenticado / token inválido o expirado |
| 403 | Autenticado sin permiso para el recurso |
| 404 | Recurso no encontrado |
| 409 | Conflicto (por ejemplo, DNI duplicado, inscripción ya existente) |
| 422 | Violación de regla de negocio (correlatividad no cumplida, cupo agotado, fuera de fecha, etc.) |

Todas las respuestas de error siguen el contrato:
```json
{
  "codigo": "REGLA_NEGOCIO_CORRELATIVIDAD",
  "mensaje": "No cumple la correlatividad requerida para inscribirse a esta materia.",
  "timestamp": "2026-07-21T10:00:00",
  "detalles": ["Materia requerida: Programación I (aprobar)"]
}
```

## 18. Documentación interactiva

Todos los endpoints quedan documentados automáticamente vía **springdoc-openapi**, expuestos en `/swagger-ui.html` y `/v3/api-docs`, con esquemas de seguridad `bearerAuth` (JWT) declarados globalmente.

## 19. Siguiente etapa

Con el contrato de API definido, el siguiente documento (`05-modulos-frontend.md`) detalla cómo el frontend en React consume estos endpoints, organizado por módulo y dashboard según el rol del usuario.
