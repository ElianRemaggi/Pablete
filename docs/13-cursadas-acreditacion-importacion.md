# 13 — Cursadas, acreditación de notas, correlatividades de final, importación Excel y constancia

Entrega derivada de `Prompt_SIGA_Cursadas_Finales_Excel_Certificado.pdf`. Léase junto con `07` (reglas), `11` (Excel en disco) y `12` (finales).

## 1. Flujo completo

```
Alumno se inscribe a la cursada (POST /inscripciones-cursada)
  → Preceptor/Admin busca al alumno / abre la comisión y carga nota + resultado (PUT /inscripciones-cursada/{id}/calificacion)
      APROBADA  → estado de la materia = REGULAR  ("cursada aprobada")
      DESAPROBADA / PENDIENTE → no habilita nada
  → El alumno se inscribe al final (POST /inscripciones-final) si cumple los requisitos de §3
  → Preceptor/Admin registra el resultado del final (PUT /inscripciones-final/{id}/resultado)
      APROBADO  → estado de la materia = APROBADA con su nota  (cuenta como correlativa)
```

**Cursada aprobada ≠ final aprobado.** Son dos instancias con notas distintas:
la nota de cursada vive en `inscripcion_cursada.nota_cursada` (+ `resultado_cursada`), la del final en `intento_final.nota`
(+ `resultado`) y se copia a `estado_academico_materia.nota` solo al aprobar. En `estado_academico_materia`,
`REGULAR` significa *cursada aprobada* y `APROBADA` significa *final aprobado*.

## 2. Notas y resultados

| Endpoint | Rol | Body | Reglas |
|---|---|---|---|
| `PUT /inscripciones-cursada/{id}/calificacion` | PRECEPTOR, ADMINISTRADOR | `{resultado: APROBADA\|DESAPROBADA\|PENDIENTE, nota}` | APROBADA/DESAPROBADA exigen nota 1–10 (`NOTA_INVALIDA` 422); PENDIENTE no admite nota. No se califica una inscripción en BAJA. Se puede corregir: si una cursada deja de estar aprobada y no hay otra aprobada de esa materia, `REGULAR` vuelve a `CURSANDO` (un final ya aprobado **no** se toca). |
| `PUT /inscripciones-final/{id}/resultado` | PRECEPTOR, ADMINISTRADOR | `{resultado: APROBADO\|DESAPROBADO\|AUSENTE, nota}` | APROBADO/DESAPROBADO exigen nota 1–10; AUSENTE no lleva. Solo mesas PUBLICADA/CERRADA, inscripción no BAJA. Corregir un APROBADO a otro resultado devuelve la materia a `REGULAR` (si no hay otro intento aprobado). |
| `GET /inscripciones-cursada/comision/{id}` | PRECEPTOR, ADMIN | — | Inscriptos con alumno (DNI, legajo), período, nota y resultado. |
| `GET /inscripciones-cursada/me`, `/inscripciones-final/me` | ALUMNO | — | Incluyen `notaCursada`/`resultadoCursada` y `nota`/`resultado`. |

Todo cambio queda auditado (`CURSADA_CALIFICADA`, `FINAL_CALIFICADO`) con resultado y nota **antes/después**, y el usuario que calificó se guarda en la fila (`calificado_por`).
No se borra nada al corregir.

**Nota mínima de aprobación:** no existe y **no hace falta** (confirmado): la cátedra decide si el número aprueba; el personal elige *aprobado/desaprobado* y SIGA solo valida que la nota esté entre 1 y 10.

## 3. Correlatividades (servicio centralizado `RequisitosAcademicosService`)

Los validadores de ambos pipelines y las consultas delegan en este único servicio.

* **Inscripción a cursada** (sin cambios de comportamiento): `CURSAR` → requerida en `REGULAR`/`APROBADA`; `APROBAR` → requerida en `APROBADA`.
* **Inscripción a final** (nuevo `ValidadorCorrelatividadFinal`, @Order 2, junto al `ValidadorRegularidad`):
  1. cursada propia aprobada (`REGULAR` o `APROBADA`) — *"No podés inscribirte al final de X porque todavía no tenés aprobada la cursada"* (`MATERIA_NO_REGULARIZADA`);
  2. **toda** correlativa configurada de la materia, sea `CURSAR` o `APROBAR`, exige el **final aprobado** (`APROBADA`) de la materia requerida — *"…porque todavía no tenés aprobado el final de Y"* (`REGLA_NEGOCIO_CORRELATIVIDAD`, con el detalle de todo lo pendiente).

  Con una sola correlatividad `Matemática I → Matemática II` (tipo `CURSAR`) se obtiene exactamente el ejemplo del prompt: para el final de Matemática II hacen falta cursada I, cursada II y final I aprobados. **Confirmado por la institución:** para rendir el final de una materia hay que haber aprobado el final de sus correlativas (en todas las materias). La nota es solo el número; si se aprueba o no lo define la cátedra al elegir *aprobado/desaprobado*.
* `GET /alumnos/me/materias/{materiaId}/habilitacion-final` (ALUMNO) y `GET /alumnos/{id}/materias/{materiaId}/habilitacion-final` (PRECEPTOR/ADMIN): `{cursadaAprobada, habilitado, correlatividades[{cumplido…}], pendientes[]}` — alimenta "finales habilitados / qué me falta".
* Se valida siempre en el backend; si una nota se corrige, las inscripciones **futuras** reflejan el cambio (las ya hechas no se borran).

## 4. Importación masiva desde la interfaz

`POST /historial-academico/importar` (multipart `archivo`; params `confirmar` y `crearAlumnos`, ambos `false` por defecto) — PRECEPTOR/ADMIN.
`GET /historial-academico/plantilla` descarga la plantilla vacía. Pantalla: **Importar historial** (menú de preceptor y administrador).

* `confirmar=false` = **vista previa**: misma lógica que la ejecución pero sin escribir. `confirmar=true` aplica en **una transacción** (todo o nada ante un error inesperado); las filas/celdas inválidas se rechazan individualmente sin frenar el resto.
* Formato esperado: igual que `11-historial-excel.md` §2 (una hoja por carrera; fila 1 encabezados `DNI`, `APELLIDO/S`, `NOMBRE/S`, `COHORTE` y por materia `N - MATERIA CURSADA/FECHA/FINAL`; alumnos desde la fila 3). Las materias se emparejan por nombre **sin tildes ni mayúsculas** con las cargadas en SIGA.
* **Política Excel vs. base** (la base manda): el Excel solo *crea* lo que no existe o *avanza* un estado (PENDIENTE/LIBRE < CURSANDO < REGULAR < APROBADA); un estado menor o contradictorio, o una nota distinta sobre una nota ya cargada, es **CONFLICTO**: se conserva la base y se informa. `PENDIENTE` en el Excel no aporta dato. Nunca se borra nada (los archivos parciales no afectan lo ausente). Reimportar el mismo archivo da `sinCambios` (verificado con 520 alumnos).
* Rechazos con motivo: DNI inválido o duplicado en la hoja, alumno inexistente, materia inexistente/ambigua, estado no reconocido (`EQUIVALENCIA`, `RECURSA`…), nota no numérica o fuera de 1–10, nota sin estado. Los estados con nota que no son `APROBADA` ignoran la nota con advertencia. El reporte de errores se descarga como CSV desde la pantalla.
* **`crearAlumnos`** (opt-in): da de alta Usuario+Alumno de los DNI que no existen, con **contraseña inicial = DNI**, legajo = DNI y email provisorio `dni@sin-email.siga.local`. SIGA no tiene flujo de recuperación de clave: si olvidan la clave, el administrador les crea una nueva (§9). No se asigna carrera (el modelo no la guarda por alumno).
* Fechas: el modelo no tiene "fecha de aprobación"; para una materia APROBADA con fecha en el Excel, esa fecha queda en `fecha_actualizacion` del estado.
* `data/historial-academico.xlsx` y el recargado desde disco (`POST /historial-academico/recargar`) **no cambiaron**.
* Tope de archivo: 25 MB (`spring.servlet.multipart`).

**El archivo real entregado** (`RESUMEN ACADEMICO TODAS LAS CARRERAS.xlsx`) tiene las 9 hojas y 126 nombres de materia distintos en los encabezados, pero **0 alumnos** (igual que `data/historial-academico.xlsx`): no hay nada que importar todavía, y no se importó nada. Para importar datos reales hace falta (1) el archivo con los alumnos cargados y (2) que las materias estén dadas de alta en SIGA con esos mismos nombres (abreviados: `MTM 1`, `PROG 1`…).

## 5. Constancia de alumno regular

`GET /certificados/alumno-regular` (el propio alumno) y `GET /certificados/alumno/{id}/alumno-regular` (PRECEPTOR/ADMIN) devuelven el **formulario en blanco** con el formato de `ALUMNO REGULAR ISET.pdf` (`CertificadoFactory.generarConstanciaAlumnoRegular()`): marco, logos Municipalidad de General Pueyrredon + ISET (recortados del modelo, `resources/certificado/*.png`), texto del modelo y líneas punteadas para nombre, DNI, carrera, curso y fecha, más sello y firma. Lo completa **a mano** el alumno o la persona autorizada, por decisión institucional: no se cargan datos del alumno ni se evalúa regularidad. Se verificó renderizando el PDF y comparándolo con el modelo. Cada descarga se registra (`CertificadoEmitido` con hash) y se audita.

## 6. Migración

`V5__acreditacion_cursadas_y_finales.sql`: `inscripcion_cursada` (+`nota_cursada`, `resultado_cursada`, `fecha_calificacion`, `calificado_por`), `intento_final` (+`nota`, `fecha_resultado`, `calificado_por`). Todas nullable (`resultado_cursada` NULL ≡ PENDIENTE). No modifica migraciones anteriores. En el perfil `dev` (H2, `create-drop`) las columnas salen de las entidades.

## 7. Frontend

Cursadas **activas por defecto** (`VITE_CURSADAS_HABILITADAS=false` las oculta). Nuevas pantallas (preceptor y administrador): **Acreditación de cursadas** (elegir comisión, buscar por nombre/apellido/DNI/legajo, cargar/corregir nota), **Importar historial**; en *Inscriptos de la mesa*: columnas de resultado y nota y botón *Registrar/Corregir resultado*. El alumno ve el resultado y nota de cada cursada (Inscripción a cursada) y de cada final (Mis inscripciones). Los mensajes de rechazo del backend (correlativas, cursada no aprobada) se muestran tal cual al intentar inscribirse.

## 9. Crear nueva clave (administrador)

`PUT /usuarios/{id}/clave` `{nuevaClave}` — **solo ADMINISTRADOR** (403 para el resto; 404 si no existe; 400 si no cumple 8 caracteres + mayúscula + número). Reemplaza la clave olvidada (la anterior deja de funcionar), **cierra todas las sesiones** (revoca los refresh tokens) y audita `USUARIO_CLAVE_RESTABLECIDA` sin guardar la clave. Pantalla: Administrador → Usuarios → botón de la llave (*Crear nueva clave*). La clave se entrega en persona.

## 10. Pruebas

`AcreditacionCursadasFinalesIT` (6), `HistorialImportacionIT` (7, incluye 520 alumnos), `ConstanciaAlumnoRegularIT` (3), `NuevaClaveIT` (2), más tests de unidad adaptados y `CertificadoFactoryTest`. Última corrida de `mvnw verify` (JDK 17, Windows, sin Docker): **151 tests de unidad + 105 de integración, 0 fallas** (antes de esta entrega: 87 de integración). Además se levantó el jar real (`java -jar … --server.port=8091`) y se verificó con curl: alumno recibe 403 al calificarse, preceptor califica (200), nota ausente → 422 `NOTA_INVALIDA`, la habilitación del final pasa a `habilitado:true`, la constancia (formulario en blanco) baja como `application/pdf` y la plantilla del importador como `.xlsx`. No se probó el frontend en un navegador (solo `tsc -b` + `vite build` limpios) ni MySQL/Flyway (la migración V5 es solo `ALTER TABLE … ADD COLUMN`).
