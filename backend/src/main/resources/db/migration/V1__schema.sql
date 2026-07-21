-- =========================================================
-- SIGA - Migracion inicial (docs/02-modelo-datos.md SS5)
-- Solo esquema. Sin datos semilla: los usuarios de prueba de
-- docs/02 SS6 son solo para dev/test, nunca para produccion.
-- =========================================================

-- ROLES Y USUARIOS -----------------------------------------
CREATE TABLE rol (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(30) NOT NULL UNIQUE,
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

-- OFERTA ACADEMICA -------------------------------------------
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
  cuatrimestre TINYINT NOT NULL,
  PRIMARY KEY (plan_id, materia_id),
  FOREIGN KEY (plan_id) REFERENCES plan_estudio(id),
  FOREIGN KEY (materia_id) REFERENCES materia(id)
);

CREATE TABLE correlatividad (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  materia_id BIGINT NOT NULL,
  materia_requerida_id BIGINT NOT NULL,
  tipo_requisito ENUM('CURSAR', 'APROBAR') NOT NULL,
  UNIQUE (materia_id, materia_requerida_id, tipo_requisito),
  CHECK (materia_id <> materia_requerida_id),
  FOREIGN KEY (materia_id) REFERENCES materia(id),
  FOREIGN KEY (materia_requerida_id) REFERENCES materia(id)
);

CREATE TABLE comision (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  materia_id BIGINT NOT NULL,
  nombre VARCHAR(20) NOT NULL,
  periodo_lectivo VARCHAR(10) NOT NULL,
  cupo_maximo INT NOT NULL,
  UNIQUE (materia_id, nombre, periodo_lectivo),
  FOREIGN KEY (materia_id) REFERENCES materia(id)
);

CREATE TABLE horario (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  comision_id BIGINT NOT NULL,
  dia_semana TINYINT NOT NULL,
  hora_inicio TIME NOT NULL,
  hora_fin TIME NOT NULL,
  aula VARCHAR(30),
  CHECK (hora_fin > hora_inicio),
  FOREIGN KEY (comision_id) REFERENCES comision(id)
);

-- INSCRIPCION DEL ALUMNO A CARRERA ----------------------------
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

-- ESTADO ACADEMICO --------------------------------------------------
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

-- CONFIGURACION INSTITUCIONAL -----------------------------------------
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

-- AUDITORIA -------------------------------------------------------------
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

-- INDICES -------------------------------------------------------------------
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

-- Seed minimo indispensable para operar en produccion (no son datos de prueba):
INSERT INTO rol (nombre, descripcion) VALUES
('ALUMNO', 'Rol de alumno'),
('PRECEPTOR', 'Rol de preceptor'),
('ADMINISTRADOR', 'Rol de administrador');

-- Fila unica de configuracion, con valores placeholder: el administrador debe ajustarlas
-- (PUT /api/v1/configuracion) apenas se despliega, antes de habilitar inscripciones reales.
INSERT INTO configuracion_institucional
(id, fecha_apertura_cursadas, fecha_cierre_cursadas, fecha_apertura_finales, fecha_cierre_finales,
 limite_intentos_final, horas_bloqueo_final)
VALUES (1,
        DATE_ADD(NOW(), INTERVAL -30 DAY), DATE_ADD(NOW(), INTERVAL 60 DAY),
        DATE_ADD(NOW(), INTERVAL -30 DAY), DATE_ADD(NOW(), INTERVAL 90 DAY),
        3, 48);
