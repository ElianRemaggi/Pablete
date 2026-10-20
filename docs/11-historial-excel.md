# 11 — Historial académico en Excel (`historial-academico.xlsx`)

Integración con el libro Excel que la secretaría ya usa para llevar el historial académico. SIGA lo **lee al
arrancar**, lo **re-lee a pedido** del administrador y **escribe de vuelta** los cambios académicos que se hacen
desde el sistema. Código: paquete `com.instituto.siga.excel`.

## 1. Ubicación y configuración

| Qué | Valor |
|---|---|
| Ubicación por defecto | `data/historial-academico.xlsx` **en la raíz del proyecto** (la carpeta que contiene `backend/` y `frontend/`) |
| Propiedad | `app.historial.excel-path` (`application.yml`) |
| Variable de entorno | `HISTORIAL_EXCEL_PATH` (ruta absoluta o relativa) |
| Plantilla vacía de respaldo | `backend/src/main/resources/historial-academico-plantilla.xlsx` (versionada, sin alumnos) |

**Cómo se resuelve la ruta** (`HistorialExcelPath`): una ruta **absoluta** se respeta tal cual. Una ruta
**relativa** (el default `./data/historial-academico.xlsx`) no depende de desde dónde se arranque: si el directorio
de trabajo es la raíz del proyecto o la carpeta `backend/`, se ancla a la raíz del proyecto (la que contiene
`backend/pom.xml`); en cualquier otro caso se resuelve contra el directorio de trabajo. Así `mvnw spring-boot:run`
desde `backend/` y desde la raíz leen **el mismo** archivo. La ruta efectiva se informa en el log de arranque y en
el campo `archivo` de la respuesta de `POST /api/v1/historial-academico/recargar`.

Para usar otro archivo:

```bash
# Git Bash
export HISTORIAL_EXCEL_PATH="C:/datos/siga/historial-academico.xlsx"
```
```powershell
# PowerShell (solo para esa sesión)
$env:HISTORIAL_EXCEL_PATH = "C:\datos\siga\historial-academico.xlsx"
```

`/data/` está en `.gitignore`: una vez cargado, el archivo tiene DNI y notas reales y **no se versiona**.

## 2. Estructura (tomada del ejemplo original `RESUMEN ACADEMICO TODAS LAS CARRERAS.xlsx`)

El archivo del repositorio conserva **las 9 hojas y todos los encabezados del ejemplo original**, sin ningún
alumno. Una hoja = una carrera/plan; el nombre de la hoja es `CÓDIGO_RESOLUCIÓN_AÑO`:

`ALIM_R11297_97`, `PROG_R6175_03`, `ENF_R854_16`, `EMER_R42_17`, `LAC_R205_18`, `IQUI_R5620_19`,
`SIST_R6790_19`, `CDIA_R2730_22`, `ALIM_R5563_23`.

Dentro de cada hoja:

| Fila (Excel) | Contenido |
|---|---|
| 1 | Encabezado: `DNI` (en `PROG`, `ENF`, … la celda A1; en `ALIM_R11297_97` está en blanco), `APELLIDO/S`, `NOMBRE/S`, `COHORTE` y, **por cada materia del plan, tres columnas consecutivas**: `N - MATERIA CURSADA`, `N - MATERIA FECHA`, `N - MATERIA FINAL`. Después, columnas de resumen (`PROMEDIO`, `CANTIDAD MATERIAS APROBADAS`, `TOTAL FINALES …`). |
| 2 | Rótulos `DNI` / `APELLIDO/S` / `NOMBRE/S` (la etiqueta `DNI` de la columna A está acá en `ALIM_R11297_97`). **No es una fila de datos.** |
| 3 en adelante | **Un alumno por fila.** Las filas vacías intercaladas se ignoran (no tienen DNI). |

Significado de las tres columnas de cada materia:

| Columna | Contenido | Ejemplo |
|---|---|---|
| `… CURSADA` | Estado de la materia para el alumno | `APROBADA`, `REGULAR`, `CURSANDO`, `LIBRE`, `PENDIENTE` |
| `… FECHA` | Fecha del final / aprobación | fecha de Excel (`25-11-25`) |
| `… FINAL` | Nota del final (1–10) | `8` |

Lo que SIGA reconoce como estado es el enum `EstadoMateria` (`PENDIENTE`, `CURSANDO`, `REGULAR`, `APROBADA`,
`LIBRE`). El archivo original usa además `EQUIVALENCIA`, `RECURSA` y la marca `-----------` (sin dato): ver §7.

### Cómo agregar o modificar registros a mano

1. Cerrá el archivo en Excel y, de preferencia, hacé una copia.
2. En la hoja de la carrera, escribí el alumno en la **primera fila libre desde la fila 3**: `DNI` (solo dígitos, como
   número o texto), `APELLIDO/S`, `NOMBRE/S`, `COHORTE`.
3. En las columnas de cada materia: `CURSADA` con **uno de los estados de arriba (en mayúsculas)**, `FECHA` y `FINAL`.
4. **No cambies ni reordenes los encabezados de la fila 1**: el nombre de materia (sin el `N - ` del orden) es lo que
   enlaza la columna con la materia cargada en SIGA.
5. Guardá, cerrá Excel y en SIGA (administrador → Configuración) usá **Recargar historial desde Excel**, o reiniciá
   el backend.

## 3. Qué se lee y qué se escribe

**Se lee** (Excel → base), al iniciar el backend (`ApplicationReadyEvent`) y con `POST /api/v1/historial-academico/recargar`
(solo ADMINISTRADOR, auditado): por cada fila con DNI, el alumno (se busca por DNI exacto entre los alumnos ya
existentes) y por cada materia con datos, el `EstadoAcademicoMateria` (estado + nota). Los datos de acceso
(contraseñas, tokens) **nunca** están ni se leen del Excel.

**Se escribe** (base → Excel): cuando un administrador fija el estado académico de un alumno en una materia
(`PUT /api/v1/alumnos/{id}/estado-academico/{materiaId}`), se escribe en la fila de ese DNI el estado y la nota, y la
fecha (solo si el estado es `APROBADA`). La respuesta incluye `excelSincronizado` y `excelMensaje`: si el alumno o la
materia **no figuran en el Excel**, o el archivo está bloqueado/no existe, el cambio queda guardado en la base y el
mensaje lo dice claramente (no se finge éxito). **No se crean filas ni hojas nuevas**: un alumno nuevo se agrega a mano
(§2). La inscripción a un final **no** escribe en el Excel (no cambia el historial académico).

**Escritura segura:** se arma el libro en memoria, se vuelca a `…xlsx.tmp`, se copia el original a `…xlsx.bak` y recién
entonces se reemplaza el archivo. Si falla (por ejemplo, Excel lo tiene abierto) el original queda intacto y el `.tmp`
se descarta. Un único candado serializa lecturas y escrituras dentro del proceso.

## 4. Fuente de verdad y resolución de conflictos

| Información | Fuente de verdad |
|---|---|
| Historial académico **anterior** a SIGA (estados y notas por materia) | **Excel** (se importa) |
| Usuarios, roles, mesas, inscripciones, comprobantes, auditoría | **Base de datos** (nunca están en el Excel) |
| Cambios académicos hechos desde SIGA | **Base de datos**, reflejados de vuelta en el Excel |

Reglas del importador (`HistorialExcelSyncService`), todas verificadas por tests:

- **Registro nuevo** (no existe en la base): se crea.
- **Registro existente**: se aplica lo que dice el Excel, **salvo conflicto**: si la base ya tiene la materia `APROBADA` y
  el Excel dice otro estado o una nota distinta, **se conserva la base**, se cuenta en `conflictos` y se lista como
  advertencia `CONFLICTO …`. Nunca se degrada ni se pisa una aprobación en silencio.
- **Celda en blanco / `-----`**: no borra nada de la base. **Nota ausente**: no borra la nota existente.
- **Fila eliminada del Excel**: no se borra nada de la base (el importador solo agrega o actualiza).
- **Duplicados**: el upsert es por (alumno, materia); sincronizar N veces da el mismo resultado (la segunda corrida informa
  `actualizados = 0`).
- **DNI sin alumno en el sistema** / **DNI que coincide con más de un alumno** / **materia inexistente o ambigua**: no se
  importa esa celda; se cuenta (`alumnosNoEncontrados`, `materiasNoEncontradas`) y se lista (hasta 50 advertencias).
- **Problemas de estructura** (materia repetida, columnas FECHA/FINAL ausentes): se informan como advertencia.

## 5. Qué pasa al reiniciar el backend

- **Perfil `dev` (H2 en memoria, el default):** la base **se reconstruye vacía en cada arranque** (`create-drop` +
  `data.sql`). Se pierde todo lo creado desde la interfaz (usuarios registrados, mesas, inscripciones) y queda solo lo
  sembrado (3 usuarios demo y la oferta de ejemplo). **Lo único que persiste entre reinicios es el Excel**, y de él solo
  se re-importan los alumnos que existan en la base en ese momento (los demás figuran en `alumnosNoEncontrados`).
- **Perfil `prod` (MySQL + Flyway):** usuarios, mesas e inscripciones persisten en MySQL; el Excel se re-sincroniza al
  arrancar según las reglas de §4.

## 6. Si el archivo se daña o desaparece

- **Falta el archivo:** al arrancar, SIGA lo crea **vacío** copiando la plantilla empaquetada (mismas hojas y
  encabezados) — nunca pisa uno existente.
- **Está dañado / no abre:** el backend sigue funcionando; la sincronización informa el error (`advertencias`) y los
  cambios se guardan solo en la base. Recuperación: (1) cerrá Excel; (2) probá con la última copia
  `historial-academico.xlsx.bak` (se genera en cada escritura): renombrala quitando `.bak`; (3) si tampoco sirve, borrá el
  archivo dañado y reiniciá (se crea vacío) o copiá a mano `backend/src/main/resources/historial-academico-plantilla.xlsx`
  a la ruta configurada; luego re-cargá los alumnos desde tu fuente.
- **Está abierto en Excel:** la lectura suele funcionar, pero la escritura falla con un mensaje claro y el original no se
  modifica. Cerralo y repetí el cambio.

## 7. Limitaciones conocidas (no se inventaron reglas)

- Los estados `EQUIVALENCIA` y `RECURSA` del archivo original **no los modela SIGA**: se informan una vez por valor y no
  cambian el estado. Si deben contar como aprobada/cursando, hace falta una decisión institucional.
- El nombre de materia del encabezado debe coincidir (ignorando mayúsculas y el prefijo `N - `) con el **nombre** de una
  Materia cargada en SIGA (`MTM 1`, `ALG`, …). Los nombres del ejemplo original son abreviaturas: hay que cargar las
  materias con esos mismos nombres (o ajustar el encabezado).
- Una materia repetida en la misma hoja (en `PROG_R6175_03`, `EDI` figura 3 veces: columnas 8, 17 y 24) no se distingue por
  nombre: se usa **la primera columna** y se informa.
- El encabezado `15 - SO FECHAS` (plural) del original se tolera.
- `Alumno` no tiene carrera mapeada en el modelo, así que SIGA no puede decidir en qué hoja dar de alta a un alumno nuevo:
  por eso no crea filas.
- Las fórmulas de resumen (`PROMEDIO`, …) no están en las filas de la plantilla; Excel las recalcula si se copian desde una fila existente.

## 8. Pruebas

`HistorialExcelPlantillaTest` (archivo real en solo lectura + copias temporales con un alumno `TEST-EXCEL`, DNI `99000001`),
`HistorialExcelPathTest`, `HistorialExcelBootstrapTest`, `HistorialExcelReaderTest` y `HistorialExcelSyncIT`
(importación, idempotencia, conflicto, DNI desconocido, escritura de vuelta y archivo inexistente, por la API real sobre una copia
temporal). Ninguna toca `data/historial-academico.xlsx`.
