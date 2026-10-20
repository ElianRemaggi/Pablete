-- Primera entrega (examenes finales): ciclo de vida de la mesa y datos de publicacion.
-- Las mesas ya existentes quedan PUBLICADAS (antes eran inscribibles por todos), asi que el default
-- preserva su comportamiento; las mesas nuevas creadas por la API nacen en BORRADOR desde el servicio.
-- Los enums se declaran como ENUM de MySQL, igual que `turno`, para que ddl-auto=validate los acepte.
ALTER TABLE mesa_examen
  ADD COLUMN estado ENUM('BORRADOR','PUBLICADA','CERRADA','CANCELADA') NOT NULL DEFAULT 'PUBLICADA',
  ADD COLUMN hora TIME NULL,
  ADD COLUMN periodo VARCHAR(20) NULL,
  ADD COLUMN aula VARCHAR(100) NULL,
  ADD COLUMN modalidad ENUM('PRESENCIAL','VIRTUAL') NULL,
  ADD COLUMN apertura_inscripcion DATETIME NULL,
  ADD COLUMN cierre_inscripcion DATETIME NULL;

CREATE INDEX idx_mesa_examen_estado ON mesa_examen(estado);
