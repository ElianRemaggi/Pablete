-- =========================================================
-- SIGA - Calendario academico, profesor y comprobantes de inscripcion
-- Migracion puramente aditiva: no toca filas ni columnas existentes,
-- salvo el ENUM de certificado_emitido.tipo que se amplia (ALTER ... MODIFY).
-- =========================================================

-- Datos de profesor sobre lo que ya dicta la clase / toma el examen. Nullable
-- porque no todas las comisiones/mesas cargadas hasta ahora lo tienen.
ALTER TABLE comision    ADD COLUMN profesor VARCHAR(150) NULL;
ALTER TABLE mesa_examen ADD COLUMN profesor VARCHAR(150) NULL;

-- Comprobantes de inscripcion (cursada/final): se emiten con el mismo mecanismo
-- que los certificados existentes (CertificadoFactory + CertificadoEmitido + hash SHA-256).
ALTER TABLE certificado_emitido MODIFY tipo
  ENUM('ALUMNO_REGULAR','ANALITICO_PARCIAL','COMPROBANTE_CURSADA','COMPROBANTE_FINAL') NOT NULL;

-- Eventos academicos institucionales (cursadas, finales e inscripciones NO se
-- duplican aca: se derivan en tiempo real de inscripcion_cursada/inscripcion_final/
-- horario/mesa_examen/configuracion_institucional). Esta tabla solo cubre lo que
-- no tiene un origen existente: eventos institucionales sueltos y fechas limite
-- cargadas a mano por el administrador.
CREATE TABLE evento_academico (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  titulo VARCHAR(150) NOT NULL,
  descripcion VARCHAR(500) NULL,
  tipo ENUM('EVENTO_ACADEMICO','FECHA_LIMITE') NOT NULL,
  fecha_inicio DATETIME NOT NULL,
  fecha_fin DATETIME NULL,
  creado_por BIGINT NULL,
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (creado_por) REFERENCES usuario(id)
);

CREATE INDEX idx_evento_academico_fecha_inicio ON evento_academico(fecha_inicio);
