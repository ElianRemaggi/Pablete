# 12 — Primera entrega: exámenes finales + historial en Excel

Alcance de esta entrega (definido en `Prompt_SIGA_Examenes_Finales_y_Excel.pdf`): **gestión de mesas de examen final,
inscripción de alumnos, comprobantes PDF y la integración con el Excel del historial académico**. Las cursadas anuales,
la inscripción a materias, las comisiones/horarios y las correlatividades se **conservan en el código** pero quedan fuera
de la interfaz de usuario.

## 1. Cómo levantarlo (sin Docker, Windows / Git Bash)

Requisitos: JDK 17+ (`java -version`), Node 18+. Maven no se instala: se usa el wrapper.

```bash
# Terminal 1 — backend (http://localhost:8080)
cd backend
./mvnw spring-boot:run

# Terminal 2 — frontend (http://localhost:5173)
cd frontend
npm install        # solo la primera vez
npm run dev
```

Si el puerto 8080 está ocupado: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` y crear
`frontend/.env.development.local` con `VITE_API_BASE_URL=http://localhost:8081/api/v1` (y `CORS_ALLOWED_ORIGINS` si el
frontend no corre en `5173`).

Variables de entorno opcionales: `HISTORIAL_EXCEL_PATH` (ruta del Excel, ver `docs/11-historial-excel.md`),
`JWT_SECRET`, `CORS_ALLOWED_ORIGINS`. Frontend: `VITE_API_BASE_URL`, `VITE_CURSADAS_HABILITADAS` (ver §6). No hay otros
archivos externos: el Excel `data/historial-academico.xlsx` se crea vacío solo si falta.

Usuarios de prueba (perfil `dev`, se siembran solos; login por **DNI**):

| DNI | Contraseña | Rol |
|---|---|---|
| `30111222` | `Admin1234` | ADMINISTRADOR (gestiona mesas) |
| `30222333` | `Preceptor1234` | PRECEPTOR (consulta mesas e inscriptos) |
| `30333444` | `Alumno1234` | ALUMNO (`alumno1`, regular en *Programación II*, que tiene una mesa publicada) |

## 2. Roles ("administrativo")

El modelo tiene tres roles: `ADMINISTRADOR`, `PRECEPTOR`, `ALUMNO`. **"Administrativo" se mapeó así**: el
**ADMINISTRADOR** crea, modifica, publica, cierra y cancela mesas y cambia estados académicos; el **PRECEPTOR** consulta mesas, inscriptos y
comprobantes, y exporta listados (solo lectura). Decisión conservadora: no se le dieron al preceptor permisos de escritura sobre mesas
(`@PreAuthorize` en los servicios, igual que el resto del sistema) porque el diseño original no se los daba; si la institución
quiere que el preceptor también publique mesas, es un cambio de una línea en `MesaExamenServiceImpl`.

## 3. Funcionamiento

### Ciclo de vida de una mesa

`BORRADOR → PUBLICADA ⇄ CERRADA`, y `CANCELADA` desde cualquiera de las anteriores (final, sin vuelta atrás).

- **Crear** (ADMIN): materia, fecha (no anterior a hoy), hora, turno, período, aula/sede, modalidad, cupo (≥ 1), profesor y,
  opcionalmente, **ventana de inscripción propia** (apertura y cierre *juntos*, cierre posterior a la apertura y no posterior al inicio del examen). Nace en `BORRADOR`: los alumnos no la ven.
- **Publicar** habilita la inscripción (también *reabre* una mesa `CERRADA`). **Cerrar** corta la inscripción por decisión
  administrativa conservando a los inscriptos. **Cancelar** pasa la mesa a `CANCELADA` y da de baja (`BAJA`, con fecha) a sus inscripciones activas
  liberando el intento de final de cada alumno: **no se borra nada** del historial.
- **Modificar**: solo en `BORRADOR`/`PUBLICADA`. Con inscriptos no se puede cambiar la materia ni bajar el cupo por debajo de la cantidad de inscriptos.
- **Eliminar** (físico): solo borradores sin inscripciones; el resto se cancela.
- **Ventana efectiva**: la propia de la mesa si la definió; si no, el período general de finales de *Configuración* (decisión documentada: la regla
  institucional ya existente es esa ventana general, no se inventó otra).

### Inscripción del alumno (`POST /api/v1/inscripciones-final`)

Se aplican, en orden: mesa **PUBLICADA** → regularidad (materia `REGULAR`/`APROBADA`) → superposición de mesas → límite de intentos →
antelación mínima (48 h configurables) → **ventana de inscripción** → **cupo**. Antes: no duplicar (409). El alumno sale **siempre del token** (no se puede inscribir a
otra persona manipulando la petición). El cupo se protege con un **lock pesimista sobre la mesa** (`SELECT … FOR UPDATE`) dentro de la transacción: solicitudes
simultáneas no pueden superar el cupo (test concurrente incluido). Los errores devuelven `codigo` + `mensaje` concretos (`MESA_NO_DISPONIBLE`,
`FUERA_DE_PLAZO`, `CUPO_AGOTADO`, `MATERIA_NO_REGULARIZADA`, `INSCRIPCION_DUPLICADA`, …).

La **cancelación** (`DELETE /api/v1/inscripciones-final/{id}`) respeta la antelación mínima; la inscripción pasa a `BAJA` y se **conserva** (el comprobante
refleja "dada de baja"). Una inscripción ya dada de baja no se puede volver a cancelar ni modificar.

### Comprobante PDF

`GET /api/v1/certificados/inscripcion-final/{id}` (alumno, solo la propia → si no es suya, 404) y
`GET /api/v1/certificados/alumno/{alumnoId}/inscripcion-final/{id}` (ADMIN/PRECEPTOR). `application/pdf`, OpenPDF (la biblioteca existente). Incluye
alumno, DNI, legajo, carrera, materia, fecha, hora, turno, período, aula, modalidad, profesor, estado de la mesa, **estado de la inscripción**,
fecha de inscripción, N.° de comprobante (`FIN-000123-2026`) e identidad institucional (ISET). El frontend lo descarga con nombre
`comprobante-final-<id>.pdf` y muestra el mensaje real del backend si falla.

## 4. Endpoints principales

| Acción | Endpoint | Rol |
|---|---|---|
| Registro de alumno | `POST /api/v1/auth/registro` | público (solo crea `ALUMNO`) |
| Login / refresh / logout | `POST /api/v1/auth/login` · `/refresh` · `/logout` | público / autenticado |
| Mis datos / cambiar mi email | `GET /api/v1/usuarios/me` · `PUT /api/v1/usuarios/me` · `GET /api/v1/alumnos/me` | autenticado |
| Listar mesas (alumno: solo publicadas) | `GET /api/v1/mesas-examen?materiaId&fecha&estado&periodo` | autenticado |
| Ver mesa | `GET /api/v1/mesas-examen/{id}` | autenticado |
| **Crear** mesa | `POST /api/v1/mesas-examen` | ADMIN |
| Modificar | `PUT /api/v1/mesas-examen/{id}` | ADMIN |
| Publicar / cerrar / cancelar | `PATCH /api/v1/mesas-examen/{id}/publicar` · `/cerrar` · `/cancelar` | ADMIN |
| Eliminar borrador | `DELETE /api/v1/mesas-examen/{id}` | ADMIN |
| **Inscribirse** | `POST /api/v1/inscripciones-final` `{ "mesaId": n }` | ALUMNO |
| **Mis inscripciones** (historial completo) | `GET /api/v1/inscripciones-final/me` | ALUMNO |
| Cancelar mi inscripción | `DELETE /api/v1/inscripciones-final/{id}` | ALUMNO |
| **Inscriptos de una mesa** | `GET /api/v1/inscripciones-final/mesa/{mesaId}` | ADMIN / PRECEPTOR |
| Listado en Excel de inscriptos | `GET /api/v1/exportaciones/inscripciones-final?mesaId=` | ADMIN / PRECEPTOR |
| **Comprobante PDF** | `GET /api/v1/certificados/inscripcion-final/{id}` | ALUMNO (propio) |
| Comprobante PDF de un alumno | `GET /api/v1/certificados/alumno/{alumnoId}/inscripcion-final/{id}` | ADMIN / PRECEPTOR |
| Recargar historial desde Excel | `POST /api/v1/historial-academico/recargar` | ADMIN |

## 5. Probarlo a mano

1. Entrá como **administrador** (`30111222`/`Admin1234`) → *Mesas de examen* → **Nueva mesa** (materia, fecha futura, cupo). Queda en
   *Borrador*; **Publicar**.
2. En otra ventana/perfil entrá como **alumno** (`30333444`/`Alumno1234`) → *Exámenes finales*: se ve la mesa publicada →
   **Inscribirme** (la sembrada de *Programación II* ya tiene a `alumno1` inscripto; una nueva materia exige que el alumno sea
   *Regular*, desde el administrador: *Buscar alumnos → ficha*, o `PUT /alumnos/{id}/estado-academico/{materiaId}`).
3. *Mis inscripciones*: estado, **Comprobante PDF**, **Cancelar inscripción**.
4. De vuelta como administrador: *Mesas de examen → Inscriptos* (listado, PDF por alumno, exportar a Excel).
5. Alumno nuevo: *Registrate* en la pantalla de login (DNI, nombre, apellido, email, contraseña ≥ 8 con mayúscula y número).

## 6. Funcionalidades de cursadas conservadas pero fuera de la interfaz

Conservado **íntegro** (nada se borró): entidades `Comision`, `Horario`, `InscripcionCursada`, `Correlatividad`; servicios, controladores y repositorios
(`/api/v1/comisiones`, `/inscripciones-cursada`, `/correlatividades`), validadores de cursada, migraciones y tests; y en el frontend las páginas
`ComisionesPage`, `CorrelatividadesPage`, `InscripcionCursadaPage` y el stub de Horarios. Fuera de la interfaz: sus ítems de menú no se muestran y sus
rutas (`/alumno/inscripcion-cursada`, `/administrador/comisiones`, `/administrador/correlatividades`, `/administrador/horarios`) muestran una pantalla
*"Funcionalidad no disponible en esta versión"*. **Para reactivarlas**: `VITE_CURSADAS_HABILITADAS=true` (p. ej. en `frontend/.env.development.local`) y reiniciar Vite
(`frontend/src/config/features.ts`). Los **endpoints del backend siguen activos** (protegidos por rol como antes): la restricción de esta entrega es de interfaz.

## 7. Pruebas

```bash
cd backend && ./mvnw verify        # unitarios (Surefire) + integración *IT (Failsafe)
cd frontend && npm run build       # tsc -b + vite build
```

Tests nuevos de esta entrega: `MesaExamenServiceImplTest`, `ExamenesFinalesFlujoIT` (ciclo de mesa, permisos, cupo, concurrencia, ventana, duplicados,
cancelación con historial, PDF y propiedad), `RegistroControllerIT`, `HistorialExcel*` (ver `docs/11-historial-excel.md` §8).
