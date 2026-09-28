-- Nota numerica (1-10) del final por materia, poblada por la sincronizacion con el historial
-- academico en Excel (ver com.instituto.siga.excel). Nullable: la mayoria de los estados
-- (CURSANDO, PENDIENTE) no tienen nota todavia.
ALTER TABLE estado_academico_materia ADD COLUMN nota INT NULL;
