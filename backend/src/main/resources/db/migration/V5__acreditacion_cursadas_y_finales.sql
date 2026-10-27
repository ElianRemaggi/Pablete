-- Acreditacion de cursadas (nota + resultado por inscripcion) y nota del final por intento.
-- Cursada y final son instancias distintas: la nota de cursada vive en inscripcion_cursada,
-- la del final en intento_final (estado_academico_materia.nota sigue siendo la nota del final vigente).
-- resultado_cursada NULL equivale a PENDIENTE (sin calificar).
ALTER TABLE inscripcion_cursada ADD COLUMN nota_cursada INT NULL;
ALTER TABLE inscripcion_cursada ADD COLUMN resultado_cursada VARCHAR(20) NULL;
ALTER TABLE inscripcion_cursada ADD COLUMN fecha_calificacion DATETIME NULL;
ALTER TABLE inscripcion_cursada ADD COLUMN calificado_por BIGINT NULL;

ALTER TABLE intento_final ADD COLUMN nota INT NULL;
ALTER TABLE intento_final ADD COLUMN fecha_resultado DATETIME NULL;
ALTER TABLE intento_final ADD COLUMN calificado_por BIGINT NULL;
