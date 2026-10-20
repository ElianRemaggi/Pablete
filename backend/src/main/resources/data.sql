-- Seed de desarrollo (perfil dev, H2). Contraseñas en texto plano para pruebas manuales:
-- admin / Admin1234 · preceptor1 / Preceptor1234 · alumno1 / Alumno1234
-- Hashes generados con BCrypt real (costo 12), no el placeholder de docs/02-modelo-datos.md.

INSERT INTO rol (nombre, descripcion) VALUES
('ALUMNO', 'Rol de alumno'),
('PRECEPTOR', 'Rol de preceptor'),
('ADMINISTRADOR', 'Rol de administrador');

-- fecha_creacion se completa a mano: @CreatedDate solo actua via JPA, no en SQL crudo.
INSERT INTO usuario (dni, email, password_hash, nombre, apellido, activo, fecha_creacion) VALUES
('30111222', 'admin@instituto.edu.ar', '$2b$12$0xtKb2P4DA1dtPfqBrous.scjEWyaYCC4aOUdalxhCqwhTTTfhcWi', 'Ana', 'Administradora', true, CURRENT_TIMESTAMP),
('30222333', 'preceptor1@instituto.edu.ar', '$2b$12$XXf54q9nj/UR0PdQsohgb.ZyEAphXK2CpWHeMzxfREszHO5yboaRC', 'Luis', 'Preceptor', true, CURRENT_TIMESTAMP),
('30333444', 'alumno1@instituto.edu.ar', '$2b$12$SkXt9JI3DDZndMZgnloC9u8TrNIihb5nqvI2uz9pwAkmXpESrAgBu', 'Juan', 'Pérez', true, CURRENT_TIMESTAMP);

INSERT INTO usuario_rol (usuario_id, rol_id) VALUES
(1, (SELECT id FROM rol WHERE nombre = 'ADMINISTRADOR')),
(2, (SELECT id FROM rol WHERE nombre = 'PRECEPTOR')),
(3, (SELECT id FROM rol WHERE nombre = 'ALUMNO'));

-- alumno1 (usuario_id=3) se sembro con SQL crudo antes de que existiera el alta automatica
-- de Alumno en UsuarioServiceImpl; sin esta fila, ninguna inscripcion podria referenciarlo.
INSERT INTO alumno (usuario_id, legajo, fecha_ingreso) VALUES (3, 'LEG-0003', CURRENT_DATE);

-- Ventanas de inscripcion abiertas por defecto (fechas relativas a hoy, no fijas) para que
-- el pipeline de inscripciones tenga algo utilizable apenas se levanta el entorno de dev.
INSERT INTO configuracion_institucional
(id, fecha_apertura_cursadas, fecha_cierre_cursadas, fecha_apertura_finales, fecha_cierre_finales,
 limite_intentos_final, horas_bloqueo_final)
VALUES (1,
        DATEADD('DAY', -30, CURRENT_TIMESTAMP), DATEADD('DAY', 60, CURRENT_TIMESTAMP),
        DATEADD('DAY', -30, CURRENT_TIMESTAMP), DATEADD('DAY', 90, CURRENT_TIMESTAMP),
        3, 48);

-- ============================================================
-- Oferta academica minima + inscripciones de alumno1, para que el
-- calendario, los comprobantes de inscripcion y la ficha de alumno
-- tengan algo real para mostrar apenas se levanta el entorno de dev
-- (docs/02-modelo-datos.md SS6, adaptado con fechas relativas).
-- ============================================================

INSERT INTO carrera (codigo, nombre, activa) VALUES
('TDA', 'Tecnicatura en Desarrollo de Aplicaciones', true);

INSERT INTO plan_estudio (carrera_id, version, vigente) VALUES
((SELECT id FROM carrera WHERE codigo = 'TDA'), '2024', true);

INSERT INTO materia (codigo, nombre, carga_horaria, activa) VALUES
('MAT101', 'Base de Datos', 96, true),
('MAT102', 'Programación I', 128, true),
('MAT201', 'Programación II', 128, true),
('MAT202', 'Redes', 64, true);

INSERT INTO plan_materia (plan_id, materia_id, anio, cuatrimestre) VALUES
((SELECT id FROM plan_estudio WHERE version = '2024'), (SELECT id FROM materia WHERE codigo = 'MAT101'), 1, 2),
((SELECT id FROM plan_estudio WHERE version = '2024'), (SELECT id FROM materia WHERE codigo = 'MAT102'), 1, 1),
((SELECT id FROM plan_estudio WHERE version = '2024'), (SELECT id FROM materia WHERE codigo = 'MAT201'), 2, 1),
((SELECT id FROM plan_estudio WHERE version = '2024'), (SELECT id FROM materia WHERE codigo = 'MAT202'), 2, 2);

-- alumno_carrera NO se siembra: esa tabla existe en la DDL pero no tiene entidad JPA en este
-- codebase (ver CLAUDE.md), asi que Hibernate create-drop nunca la crea en dev.

INSERT INTO comision (materia_id, nombre, periodo_lectivo, cupo_maximo, profesor) VALUES
((SELECT id FROM materia WHERE codigo = 'MAT102'), 'A', '2026-1', 30, 'Marina Rossi'),
((SELECT id FROM materia WHERE codigo = 'MAT101'), 'A', '2026-1', 25, 'Diego Gómez');

INSERT INTO horario (comision_id, dia_semana, hora_inicio, hora_fin, aula) VALUES
((SELECT id FROM comision WHERE nombre = 'A' AND materia_id = (SELECT id FROM materia WHERE codigo = 'MAT102')),
 2, '18:00:00', '21:00:00', 'Aula 1'),
((SELECT id FROM comision WHERE nombre = 'A' AND materia_id = (SELECT id FROM materia WHERE codigo = 'MAT101')),
 4, '18:00:00', '21:00:00', 'Aula 3');

INSERT INTO mesa_examen (materia_id, fecha, turno, cupo_maximo, profesor, hora, periodo, aula, modalidad, estado) VALUES
((SELECT id FROM materia WHERE codigo = 'MAT201'), DATEADD('DAY', 20, CURRENT_DATE), 'MANANA', 20, 'Laura Díaz', '09:00:00', '2026-2', 'Aula 2', 'PRESENCIAL', 'PUBLICADA'),
((SELECT id FROM materia WHERE codigo = 'MAT202'), DATEADD('DAY', 35, CURRENT_DATE), 'TARDE', 20, 'Pablo Fontana', '16:00:00', '2026-2', 'Aula 4', 'PRESENCIAL', 'PUBLICADA');

-- Estado academico de alumno1 por materia: aprobada, regular (habilita el final de MAT201) y cursando.
-- fecha_actualizacion se completa a mano: en H2 (create-drop desde el mapeo JPA) la columna es
-- NOT NULL sin default, a diferencia del DEFAULT CURRENT_TIMESTAMP que tiene en el DDL de MySQL.
INSERT INTO estado_academico_materia (alumno_id, materia_id, estado, fecha_actualizacion) VALUES
(3, (SELECT id FROM materia WHERE codigo = 'MAT101'), 'APROBADA', CURRENT_TIMESTAMP),
(3, (SELECT id FROM materia WHERE codigo = 'MAT201'), 'REGULAR', CURRENT_TIMESTAMP),
(3, (SELECT id FROM materia WHERE codigo = 'MAT102'), 'CURSANDO', CURRENT_TIMESTAMP);

INSERT INTO inscripcion_cursada (alumno_id, comision_id, estado, fecha_inscripcion) VALUES
(3, (SELECT id FROM comision WHERE nombre = 'A' AND materia_id = (SELECT id FROM materia WHERE codigo = 'MAT102')),
 'INSCRIPTO', DATEADD('DAY', -5, CURRENT_TIMESTAMP));

INSERT INTO inscripcion_final (alumno_id, mesa_id, estado, fecha_inscripcion) VALUES
(3, (SELECT id FROM mesa_examen WHERE materia_id = (SELECT id FROM materia WHERE codigo = 'MAT201')),
 'INSCRIPTO', DATEADD('DAY', -2, CURRENT_TIMESTAMP));

-- Fila creada normalmente por InscripcionFinalServiceImpl.crearIntentoFinal al inscribirse.
INSERT INTO intento_final (alumno_id, materia_id, inscripcion_final_id, nro_intento, resultado, fecha) VALUES
(3, (SELECT id FROM materia WHERE codigo = 'MAT201'),
 (SELECT id FROM inscripcion_final WHERE alumno_id = 3 AND mesa_id = (SELECT id FROM mesa_examen WHERE materia_id = (SELECT id FROM materia WHERE codigo = 'MAT201'))),
 1, NULL, DATEADD('DAY', 20, CURRENT_DATE));

-- fecha_creacion se completa a mano por el mismo motivo que fecha_actualizacion arriba
-- (la entidad no tiene @CreationTimestamp; en produccion la fija el servicio, no la DB).
INSERT INTO evento_academico (titulo, descripcion, tipo, fecha_inicio, creado_por, fecha_creacion) VALUES
('Acto de colación', 'Ceremonia de entrega de diplomas para egresados 2026.', 'EVENTO_ACADEMICO',
 DATEADD('DAY', 45, CURRENT_TIMESTAMP), 1, CURRENT_TIMESTAMP),
('Cierre de inscripciones especiales', 'Última fecha para trámites de inscripción fuera de término.', 'FECHA_LIMITE',
 DATEADD('DAY', 10, CURRENT_TIMESTAMP), 1, CURRENT_TIMESTAMP);
