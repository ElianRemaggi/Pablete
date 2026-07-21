# 05. Frontend — Módulos, Rutas y Componentes

## 1. Enrutamiento (React Router)

```
/                                → redirige a /login o al dashboard según rol
/login                           → LoginPage (público)

/alumno                          → layout con Navbar + Sidebar (rol ALUMNO)
  /alumno/dashboard
  /alumno/perfil
  /alumno/historial-academico
  /alumno/inscripcion-cursada
  /alumno/inscripcion-final
  /alumno/certificados

/preceptor                       → layout (rol PRECEPTOR)
  /preceptor/dashboard
  /preceptor/busqueda-alumnos
  /preceptor/alumno/:id
  /preceptor/exportaciones

/administrador                   → layout (rol ADMINISTRADOR)
  /administrador/dashboard
  /administrador/usuarios
  /administrador/carreras
  /administrador/planes-estudio
  /administrador/materias
  /administrador/correlatividades
  /administrador/horarios
  /administrador/comisiones
  /administrador/mesas-examen
  /administrador/configuracion
  /administrador/auditoria
  /administrador/reportes

/403                              → acceso denegado
/404                              → no encontrado
```

Cada rama de rutas está protegida por un componente `RequireRole(rol)` que valida el JWT decodificado en el contexto de autenticación (`useAuth`) y redirige a `/403` si el rol no corresponde.

## 2. Layout general

- **Navbar**: logo institucional, nombre del usuario, menú de notificaciones, botón de logout.
- **Sidebar**: ítems de navegación según rol (colapsable en mobile a un drawer).
- **Contenido principal**: `<Outlet />` de React Router dentro de un contenedor con `maxWidth` responsive de MUI.

## 3. Módulo Login

- Formulario con `DNI` y `Contraseña` (React Hook Form + Zod: DNI numérico 7-9 dígitos, password mínimo 8 caracteres).
- Al loguear, guarda `accessToken`/`refreshToken` en memoria (contexto) y en `httpOnly cookie` si el backend lo soporta, evitando `localStorage` para el refresh token por seguridad.
- Interceptor de Axios: agrega `Authorization: Bearer <token>`, y ante 401 intenta un refresh automático antes de redirigir a `/login`.

## 4. Dashboard Alumno

Widgets:
- Resumen de estado académico (aprobadas / regulares / pendientes) con gráfico de progreso.
- Próximas fechas clave (cierre de inscripción a cursadas/finales) leídas de `/configuracion`.
- Accesos directos a "Inscribirme a cursada", "Inscribirme a final", "Descargar certificado".

### Inscripción a cursada (alumno)
- Tabla de materias disponibles para el período vigente (según plan del alumno y correlatividades cumplidas, resueltas en backend).
- Selector de comisión con detalle de horario; el frontend resalta superposiciones horarias contra las comisiones ya elegidas (validación optimista, la autoritativa es del backend).
- Botón "Inscribirme" → `POST /inscripciones-cursada`; errores 422 se muestran como `Alert` con el mensaje de regla de negocio.
- Tabla de "Mis inscripciones" con acción "Modificar" (habilitada sólo antes de la fecha de cierre) y "Dar de baja".

### Inscripción a final (alumno)
- Listado de materias en condición de regularidad (según `estado_academico_materia`).
- Selector de mesa de examen con fecha/turno; bloqueo de selección si faltan menos de 48hs (validado también en backend).
- Contador de intentos ya utilizados por materia, visible antes de confirmar.

### Historial académico
- Tabla filtrable por año/estado, con export a PDF del lado cliente (usa el endpoint de certificados) para el analítico parcial.

### Certificados
- Botones para "Certificado de Alumno Regular" y "Analítico Parcial", que llaman al endpoint correspondiente y disparan la descarga del blob PDF recibido.

## 5. Dashboard Preceptor

- **Buscador de alumnos**: campo de búsqueda con debounce (DNI, legajo, apellido) → tabla paginada de resultados (`GET /alumnos`).
- **Ficha de alumno**: tabs de "Estado académico", "Inscripciones a cursadas", "Inscripciones a finales", con botones para exportar y descargar certificados.
- **Exportaciones**: pantalla con filtros (comisión / mesa / carrera) que dispara la descarga del Excel generado por el backend.

## 6. Dashboard Administrador

Cada submódulo sigue el mismo patrón CRUD: tabla con búsqueda/filtros + botón "Nuevo" que abre un modal con formulario (React Hook Form), y acciones de editar/eliminar por fila con confirmación en modal.

- **Usuarios**: alta/baja/modificación, asignación de roles, activar/desactivar.
- **Carreras y Planes de Estudio**: ABM de carreras; dentro de cada carrera, ABM de planes de estudio y su malla de materias por año/cuatrimestre.
- **Materias**: ABM + asociación a planes.
- **Correlatividades**: selector de materia + materia requerida + tipo de requisito (cursar/aprobar), con validación de que no se generen ciclos.
- **Horarios y Comisiones**: ABM de comisiones con grilla semanal visual para cargar horarios y detectar superposiciones de aula/franja al momento de cargar.
- **Mesas de examen**: ABM con calendario para elegir fecha y turno.
- **Configuración institucional**: formulario único con fechas de apertura/cierre de cursadas y finales, límite de intentos, horas de bloqueo previo al final.
- **Auditoría**: tabla de sólo lectura, filtros por usuario/acción/entidad/rango de fechas, con detalle expandible por fila (JSON formateado).
- **Reportes**: paneles con gráficos (recharts) de inscriptos por comisión, ocupación de cupos, tasa de aprobación por materia.

## 7. Componentes compartidos

| Componente | Uso |
|---|---|
| `DataTable` | Tabla genérica con paginación, orden y filtros, usada en todos los listados |
| `FormModal` | Modal reutilizable para formularios de alta/edición |
| `ConfirmDialog` | Confirmación de acciones destructivas (baja, eliminación) |
| `AlertSnackbar` | Notificaciones de éxito/error, centralizadas vía contexto |
| `RoleGuard` | Wrapper de rutas protegidas por rol |
| `FileDownloadButton` | Botón genérico que dispara descarga de blobs (PDF/Excel) |
| `WeekScheduleGrid` | Grilla semanal para visualizar/cargar horarios de comisión |

## 8. Manejo de estado y datos

- Estado de servidor: se recomienda **React Query (TanStack Query)** para cachear y sincronizar los datos de la API (no listado explícitamente en el stack original pero compatible con Axios; alternativa sin esta librería es contexto + hooks custom `useFetch`).
- Estado de UI local: `useState`/`useReducer` por componente.
- Formularios: `React Hook Form` + resolver de validación (Zod o Yup).

## 9. Tema institucional (Material UI)

```ts
export const theme = createTheme({
  palette: {
    primary: { main: '#0B3D91' },     // azul institucional
    secondary: { main: '#F2A900' },   // dorado/acento
    background: { default: '#F5F6F8' }
  },
  typography: { fontFamily: 'Inter, Roboto, sans-serif' },
  shape: { borderRadius: 8 }
});
```

## 10. Responsive

- Sidebar colapsa a `Drawer` temporal en breakpoints `xs`/`sm`.
- `DataTable` cambia a modo de "cards" apiladas en mobile para listados extensos.
- Formularios usan `Grid` de MUI con columnas `xs=12 sm=6 md=4` según el campo.

## 11. Siguiente etapa

Con el frontend especificado, el siguiente documento (`06-seguridad-autenticacion.md`) detalla el esquema completo de seguridad: JWT, roles, refresh tokens, hashing y protecciones adicionales.
