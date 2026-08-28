# 02. Modelo de Datos

## 1. Modelo Entidad-Relación (descripción)

Entidades principales y sus relaciones:

- **usuario** (1) ── (1) **rol** [muchos usuarios a un rol; rol N:M vía tabla si se requieren múltiples roles por usuario — se opta por N:M para flexibilidad futura: `usuario_rol`]
- **usuario** (1) ── (0..1) **alumno** / (0..1) **preceptor** / (0..1) **administrador** (herencia por tabla separada, estrategia *Table per Subclass*)
- **carrera** (1) ── (N) **plan_estudio**
- **plan_estudio** (1) ── (N) **materia** (vía `plan_materia`, porque una materia puede pertenecer a más de un plan)
- **materia** (1) ── (N) **correlatividad** (auto-relación materia ↔ materia requerida)
- **materia** (1) ── (N) **comision**
- **comision** (1) ── (N) **horario**
- **comision** (1) ── (N) **inscripcion_cursada** ── (N) ── (1) **alumno**
- **materia** (1) ── (N) **mesa_examen**
- **mesa_examen** (1) ── (N) **inscripcion_final** ── (N) ── (1) **alumno**
- **alumno** (1) ── (N) **intento_final** (histórico de intentos por materia)
- **alumno** (1) ── (N) **estado_academico_materia** (aprobada/regular/pendiente/libre por materia)
- **alumno** (1) ── (N) **certificado_emitido** (registro de PDFs generados)
- **configuracion_institucional** (tabla singleton de parámetros globales)
- **auditoria** (N) registros, cada uno referenciando opcionalmente a `usuario`

## 2. Modelo relacional (tablas, PK, FK, restricciones)

| Tabla | PK | FKs principales | Restricciones clave |
|---|---|---|---|
| `usuario` | id | — | `dni` UNIQUE, `email` UNIQUE |
| `rol` | id | — | `nombre` UNIQUE (ALUMNO, PRECEPTOR, ADMINISTRADOR) |
| `usuario_rol` | (usuario_id, rol_id) | usuario_id→usuario, rol_id→rol | PK compuesta |
| `alumno` | usuario_id | usuario_id→usuario (1:1) | `legajo` UNIQUE |
| `preceptor` | usuario_id | usuario_id→usuario (1:1) | — |
| `administrador` | usuario_id | usuario_id→usuario (1:1) | — |
| `carrera` | id | — | `nombre` UNIQUE, `codigo` UNIQUE |
| `plan_estudio` | id | carrera_id→carrera | UNIQUE(carrera_id, version) |
| `materia` | id | — | UNIQUE(codigo) |
| `plan_materia` | (plan_id, materia_id) | plan_id→plan_estudio, materia_id→materia | incluye `anio`, `cuatrimestre` |
| `correlatividad` | id | materia_id→materia, materia_requerida_id→materia | UNIQUE(materia_id, materia_requerida_id), CHECK(materia_id <> materia_requerida_id) |
| `comision` | id | materia_id→materia | UNIQUE(materia_id, nombre, periodo_lectivo); `profesor` VARCHAR(150) NULL (agregado en la actualización de calendario/comprobantes — texto libre, no hay entidad Profesor) |
| `horario` | id | comision_id→comision | CHECK(hora_fin > hora_inicio) |
| `alumno_carrera` | (alumno_id, carrera_id) | alumno_id→alumno, carrera_id→carrera | inscripción del alumno a la carrera/plan (tabla presente en la DDL pero sin entidad JPA — no usada por el código) |
| `inscripcion_cursada` | id | alumno_id→alumno, comision_id→comision | UNIQUE(alumno_id, comision_id), estado ENUM |
| `mesa_examen` | id | materia_id→materia | UNIQUE(materia_id, fecha, turno); `profesor` VARCHAR(150) NULL (idem comisión) |
| `inscripcion_final` | id | alumno_id→alumno, mesa_id→mesa_examen | UNIQUE(alumno_id, mesa_id), estado ENUM |
| `intento_final` | id | alumno_id→alumno, materia_id→materia, inscripcion_final_id→inscripcion_final | nro_intento incremental por (alumno, materia) |
| `estado_academico_materia` | id | alumno_id→alumno, materia_id→materia | UNIQUE(alumno_id, materia_id), estado ENUM(PENDIENTE, CURSANDO, REGULAR, APROBADA, LIBRE) |
| `certificado_emitido` | id | alumno_id→alumno | tipo ENUM(ALUMNO_REGULAR, ANALITICO_PARCIAL, COMPROBANTE_CURSADA, COMPROBANTE_FINAL — los dos últimos agregados en la actualización de calendario/comprobantes), hash del PDF |
| `configuracion_institucional` | id | — | fila única (`id = 1` forzado por CHECK o patrón singleton) |
| `auditoria` | id | usuario_id→usuario (nullable) | accion, entidad, entidad_id, fecha, ip |
| `refresh_token` | id | usuario_id→usuario | token UNIQUE, expiracion, revocado |
| `evento_academico` | id | creado_por→usuario (nullable) | agregada en la actualización de calendario/comprobantes; tipo ENUM(EVENTO_ACADEMICO, FECHA_LIMITE) — cursadas/finales/inscripciones del calendario NO se persisten acá, se derivan en tiempo real de `horario`/`mesa_examen`/`inscripcion_cursada`/`inscripcion_final`/`configuracion_institucional` |

## 3. Normalización

El modelo está normalizado hasta **3FN**:

- **1FN**: todos los atributos son atómicos (por ejemplo, horarios se modelan en tabla propia `horario` en lugar de un campo de texto libre en `comision`).
- **2FN**: en tablas con PK compuesta (`plan_materia`, `usuario_rol`, `alumno_carrera`) todos los atributos no clave dependen de la clave compuesta completa (por ejemplo `anio`/`cuatrimestre` dependen del par plan+materia, no de uno solo).
- **3FN**: no hay dependencias transitivas; por ejemplo, el nombre de la carrera no se repite en `plan_estudio` (se accede vía `carrera_id`), y el estado académico consolidado no se guarda redundante en `alumno` sino derivado de `estado_academico_materia`.

Desnormalización deliberada y justificada: `certificado_emitido` guarda un snapshot (hash + metadata) del PDF generado para trazabilidad legal, aun cuando los datos de origen puedan cambiar después.

## 4. Índices

```sql
CREATE INDEX idx_usuario_dni ON usuario(dni);
CREATE INDEX idx_alumno_legajo ON alumno(legajo);
CREATE INDEX idx_materia_codigo ON materia(codigo);
CREATE INDEX idx_inscripcion_cursada_alumno ON inscripcion_cursada(alumno_id);
CREATE INDEX idx_inscripcion_cursada_comision ON inscripcion_cursada(comision_id);
CREATE INDEX idx_inscripcion_final_alumno ON inscripcion_final(alumno_id);
CREATE INDEX idx_inscripcion_final_mesa ON inscripcion_final(mesa_id);
CREATE INDEX idx_estado_academico_alumno ON estado_academico_materia(alumno_id);
CREATE INDEX idx_horario_comision ON horario(comision_id);
CREATE INDEX idx_mesa_examen_fecha ON mesa_examen(fecha);
CREATE INDEX idx_auditoria_usuario ON auditoria(usuario_id);
CREATE INDEX idx_auditoria_fecha ON auditoria(fecha);
CREATE INDEX idx_correlatividad_materia ON correlatividad(materia_id);
```

## 5. Script DDL completo (MySQL 8)

```sql
-- =========================================================
-- SIGA - Script de creación de base de datos
-- =========================================================
CREATE DATABASE IF NOT EXISTS siga_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE siga_db;

-- ROLES Y USUARIOS -----------------------------------------
CREATE TABLE rol (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(30) NOT NULL UNIQUE,           -- ALUMNO, PRECEPTOR, ADMINISTRADOR
  descripcion VARCHAR(150)
);

CREATE TABLE usuario (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  dni VARCHAR(15) NOT NULL UNIQUE,
  email VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  nombre VARCHAR(100) NOT NULL,
  apellido VARCHAR(100) NOT NULL,
  activo BOOLEAN NOT NULL DEFAULT TRUE,
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_actualizacion DATETIME NULL ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE usuario_rol (
  usuario_id BIGINT NOT NULL,
  rol_id BIGINT NOT NULL,
  PRIMARY KEY (usuario_id, rol_id),
  FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
  FOREIGN KEY (rol_id) REFERENCES rol(id) ON DELETE CASCADE
);

CREATE TABLE alumno (
  usuario_id BIGINT PRIMARY KEY,
  legajo VARCHAR(20) NOT NULL UNIQUE,
  fecha_ingreso DATE NOT NULL,
  FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);

CREATE TABLE preceptor (
  usuario_id BIGINT PRIMARY KEY,
  legajo_empleado VARCHAR(20) NOT NULL UNIQUE,
  FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);

CREATE TABLE administrador (
  usuario_id BIGINT PRIMARY KEY,
  FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);

-- OFERTA ACADÉMICA -------------------------------------------
CREATE TABLE carrera (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(20) NOT NULL UNIQUE,
  nombre VARCHAR(150) NOT NULL UNIQUE,
  activa BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE plan_estudio (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  carrera_id BIGINT NOT NULL,
  version VARCHAR(10) NOT NULL,
  vigente BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE (carrera_id, version),
  FOREIGN KEY (carrera_id) REFERENCES carrera(id)
);

CREATE TABLE materia (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(20) NOT NULL UNIQUE,
  nombre VARCHAR(150) NOT NULL,
  carga_horaria INT NOT NULL,
  activa BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE plan_materia (
  plan_id BIGINT NOT NULL,
  materia_id BIGINT NOT NULL,
  anio INT NOT NULL,
  cuatrimestre TINYINT NOT NULL,          -- 1 o 2
  PRIMARY KEY (plan_id, materia_id),
  FOREIGN KEY (plan_id) REFERENCES plan_estudio(id),
  FOREIGN KEY (materia_id) REFERENCES materia(id)
);

CREATE TABLE correlatividad (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  materia_id BIGINT NOT NULL,
  materia_requerida_id BIGINT NOT NULL,
  tipo_requisito ENUM('CURSAR', 'APROBAR') NOT NULL,  -- requiere regularizar o aprobar la correlativa
  UNIQUE (materia_id, materia_requerida_id, tipo_requisito),
  CHECK (materia_id <> materia_requerida_id),
  FOREIGN KEY (materia_id) REFERENCES materia(id),
  FOREIGN KEY (materia_requerida_id) REFERENCES materia(id)
);

CREATE TABLE comision (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  materia_id BIGINT NOT NULL,
  nombre VARCHAR(20) NOT NULL,          -- Comisión A, B...
  periodo_lectivo VARCHAR(10) NOT NULL, -- 2026-1C
  cupo_maximo INT NOT NULL,
  UNIQUE (materia_id, nombre, periodo_lectivo),
  FOREIGN KEY (materia_id) REFERENCES materia(id)
);

CREATE TABLE horario (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  comision_id BIGINT NOT NULL,
  dia_semana TINYINT NOT NULL,   -- 1=Lunes ... 7=Domingo
  hora_inicio TIME NOT NULL,
  hora_fin TIME NOT NULL,
  aula VARCHAR(30),
  CHECK (hora_fin > hora_inicio),
  FOREIGN KEY (comision_id) REFERENCES comision(id)
);

-- INSCRIPCIÓN DEL ALUMNO A CARRERA ----------------------------
CREATE TABLE alumno_carrera (
  alumno_id BIGINT NOT NULL,
  carrera_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  fecha_inscripcion DATE NOT NULL,
  PRIMARY KEY (alumno_id, carrera_id),
  FOREIGN KEY (alumno_id) REFERENCES alumno(usuario_id),
  FOREIGN KEY (carrera_id) REFERENCES carrera(id),
  FOREIGN KEY (plan_id) REFERENCES plan_estudio(id)
);

-- CURSADAS ------------------------------------------------------
CREATE TABLE inscripcion_cursada (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  alumno_id BIGINT NOT NULL,
  comision_id BIGINT NOT NULL,
  estado ENUM('INSCRIPTO','MODIFICADA','BAJA') NOT NULL DEFAULT 'INSCRIPTO',
  fecha_inscripcion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_modificacion DATETIME NULL,
  UNIQUE (alumno_id, comision_id),
  FOREIGN KEY (alumno_id) REFERENCES alumno(usuario_id),
  FOREIGN KEY (comision_id) REFERENCES comision(id)
);

-- FINALES ---------------------------------------------------------
CREATE TABLE mesa_examen (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  materia_id BIGINT NOT NULL,
  fecha DATE NOT NULL,
  turno ENUM('MANANA','TARDE','NOCHE') NOT NULL,
  cupo_maximo INT NOT NULL,
  UNIQUE (materia_id, fecha, turno),
  FOREIGN KEY (materia_id) REFERENCES materia(id)
);

CREATE TABLE inscripcion_final (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  alumno_id BIGINT NOT NULL,
  mesa_id BIGINT NOT NULL,
  estado ENUM('INSCRIPTO','MODIFICADA','BAJA') NOT NULL DEFAULT 'INSCRIPTO',
  fecha_inscripcion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_modificacion DATETIME NULL,
  UNIQUE (alumno_id, mesa_id),
  FOREIGN KEY (alumno_id) REFERENCES alumno(usuario_id),
  FOREIGN KEY (mesa_id) REFERENCES mesa_examen(id)
);

CREATE TABLE intento_final (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  alumno_id BIGINT NOT NULL,
  materia_id BIGINT NOT NULL,
  inscripcion_final_id BIGINT NOT NULL,
  nro_intento INT NOT NULL,
  resultado ENUM('AUSENTE','DESAPROBADO','APROBADO') NULL,
  fecha DATE NOT NULL,
  FOREIGN KEY (alumno_id) REFERENCES alumno(usuario_id),
  FOREIGN KEY (materia_id) REFERENCES materia(id),
  FOREIGN KEY (inscripcion_final_id) REFERENCES inscripcion_final(id)
);

-- ESTADO ACADÉMICO --------------------------------------------------
CREATE TABLE estado_academico_materia (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  alumno_id BIGINT NOT NULL,
  materia_id BIGINT NOT NULL,
  estado ENUM('PENDIENTE','CURSANDO','REGULAR','APROBADA','LIBRE') NOT NULL DEFAULT 'PENDIENTE',
  fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE (alumno_id, materia_id),
  FOREIGN KEY (alumno_id) REFERENCES alumno(usuario_id),
  FOREIGN KEY (materia_id) REFERENCES materia(id)
);

-- CERTIFICADOS -------------------------------------------------------
CREATE TABLE certificado_emitido (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  alumno_id BIGINT NOT NULL,
  tipo ENUM('ALUMNO_REGULAR','ANALITICO_PARCIAL') NOT NULL,
  fecha_emision DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  hash_documento VARCHAR(64) NOT NULL,
  FOREIGN KEY (alumno_id) REFERENCES alumno(usuario_id)
);

-- CONFIGURACIÓN INSTITUCIONAL -----------------------------------------
CREATE TABLE configuracion_institucional (
  id TINYINT PRIMARY KEY DEFAULT 1,
  fecha_apertura_cursadas DATETIME NOT NULL,
  fecha_cierre_cursadas DATETIME NOT NULL,
  fecha_apertura_finales DATETIME NOT NULL,
  fecha_cierre_finales DATETIME NOT NULL,
  limite_intentos_final INT NOT NULL DEFAULT 3,
  horas_bloqueo_final INT NOT NULL DEFAULT 48,
  CHECK (id = 1)
);

-- AUDITORÍA -------------------------------------------------------------
CREATE TABLE auditoria (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NULL,
  accion VARCHAR(100) NOT NULL,
  entidad VARCHAR(100) NOT NULL,
  entidad_id BIGINT NULL,
  detalle JSON NULL,
  ip VARCHAR(45) NOT NULL,
  fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

-- REFRESH TOKEN -----------------------------------------------------------
CREATE TABLE refresh_token (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  token VARCHAR(255) NOT NULL UNIQUE,
  expiracion DATETIME NOT NULL,
  revocado BOOLEAN NOT NULL DEFAULT FALSE,
  FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

-- ÍNDICES -------------------------------------------------------------------
CREATE INDEX idx_usuario_dni ON usuario(dni);
CREATE INDEX idx_alumno_legajo ON alumno(legajo);
CREATE INDEX idx_materia_codigo ON materia(codigo);
CREATE INDEX idx_inscripcion_cursada_alumno ON inscripcion_cursada(alumno_id);
CREATE INDEX idx_inscripcion_cursada_comision ON inscripcion_cursada(comision_id);
CREATE INDEX idx_inscripcion_final_alumno ON inscripcion_final(alumno_id);
CREATE INDEX idx_inscripcion_final_mesa ON inscripcion_final(mesa_id);
CREATE INDEX idx_estado_academico_alumno ON estado_academico_materia(alumno_id);
CREATE INDEX idx_horario_comision ON horario(comision_id);
CREATE INDEX idx_mesa_examen_fecha ON mesa_examen(fecha);
CREATE INDEX idx_auditoria_usuario ON auditoria(usuario_id);
CREATE INDEX idx_auditoria_fecha ON auditoria(fecha);
CREATE INDEX idx_correlatividad_materia ON correlatividad(materia_id);
```

## 6. Datos de prueba (seed)

```sql
INSERT INTO rol (nombre, descripcion) VALUES
('ALUMNO','Rol de alumno'), ('PRECEPTOR','Rol de preceptor'), ('ADMINISTRADOR','Rol de administrador');

INSERT INTO usuario (dni, email, password_hash, nombre, apellido) VALUES
('30111222','admin@instituto.edu.ar','$2a$10$hashDemoAdmin','Ana','Administradora'),
('30222333','preceptor1@instituto.edu.ar','$2a$10$hashDemoPrec','Luis','Preceptor'),
('30333444','alumno1@instituto.edu.ar','$2a$10$hashDemoAl1','Juan','Pérez'),
('30444555','alumno2@instituto.edu.ar','$2a$10$hashDemoAl2','María','Gómez');

INSERT INTO usuario_rol (usuario_id, rol_id) VALUES (1,3),(2,2),(3,1),(4,1);

INSERT INTO alumno (usuario_id, legajo, fecha_ingreso) VALUES (3,'LEG-0001','2023-03-01'),(4,'LEG-0002','2023-03-01');
INSERT INTO preceptor (usuario_id, legajo_empleado) VALUES (2,'EMP-0001');
INSERT INTO administrador (usuario_id) VALUES (1);

INSERT INTO carrera (codigo, nombre) VALUES ('TSW','Tecnicatura en Sistemas Web');
INSERT INTO plan_estudio (carrera_id, version) VALUES (1,'2024');

INSERT INTO materia (codigo, nombre, carga_horaria) VALUES
('MAT101','Programación I',120), ('MAT102','Programación II',120), ('MAT201','Bases de Datos',96);

INSERT INTO plan_materia (plan_id, materia_id, anio, cuatrimestre) VALUES
(1,1,1,1), (1,2,1,2), (1,3,2,1);

INSERT INTO correlatividad (materia_id, materia_requerida_id, tipo_requisito) VALUES
(2,1,'APROBAR'), (3,1,'CURSAR');

INSERT INTO comision (materia_id, nombre, periodo_lectivo, cupo_maximo) VALUES
(1,'A','2026-1C',40), (2,'A','2026-1C',35);

INSERT INTO horario (comision_id, dia_semana, hora_inicio, hora_fin, aula) VALUES
(1,1,'18:00:00','22:00:00','Aula 3'), (2,3,'18:00:00','22:00:00','Aula 4');

INSERT INTO configuracion_institucional
(id, fecha_apertura_cursadas, fecha_cierre_cursadas, fecha_apertura_finales, fecha_cierre_finales, limite_intentos_final, horas_bloqueo_final)
VALUES (1,'2026-02-01 00:00:00','2026-03-10 23:59:59','2026-06-01 00:00:00','2026-07-15 23:59:59',3,48);
```

## 7. Siguiente etapa

Con el modelo de datos definido, el siguiente documento (`03-entidades-dto.md`) detalla la traducción de este modelo a entidades JPA, DTOs de entrada/salida y mappers MapStruct.
