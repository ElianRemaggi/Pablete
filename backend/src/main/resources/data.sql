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
