# 07. Reglas de Negocio y Validaciones

## 1. Reglas de inscripción a cursadas

Un alumno puede inscribirse a una comisión **si y sólo si** se cumplen todas las condiciones siguientes (evaluadas en orden por un pipeline de validadores, patrón *Chain of Responsibility*):

1. **Correlatividades cumplidas**: para cada `Correlatividad` de la materia de la comisión, el alumno debe tener el estado requerido (`CURSAR` → materia requerida en estado `REGULAR` o `APROBADA`; `APROBAR` → materia requerida en estado `APROBADA`) en `estado_academico_materia`.
2. **Sin superposición horaria**: ningún `Horario` de la comisión elegida se solapa (mismo día, rango horario intersectante) con los `Horario` de otras comisiones donde el alumno ya tenga una `InscripcionCursada` en estado `INSCRIPTO`/`MODIFICADA` para el mismo período lectivo.
3. **Cupo disponible**: `COUNT(inscripcion_cursada WHERE comision_id = X AND estado IN (INSCRIPTO, MODIFICADA)) < comision.cupoMaximo`.
4. **Dentro del período de inscripción**: `now() BETWEEN configuracion.fechaAperturaCursadas AND configuracion.fechaCierreCursadas`.
5. **Sin inscripción duplicada**: el alumno no debe tener ya una inscripción activa a la misma comisión (restricción de unicidad además reforzada a nivel de base de datos).

**Modificación de inscripción**: permitida únicamente si `now() <= configuracion.fechaCierreCursadas`; al modificar, se re-ejecutan las reglas 1 a 3 sobre la nueva comisión elegida, y el estado pasa a `MODIFICADA`.

**Baja de inscripción**: siempre permitida antes del cierre; pasa el estado a `BAJA` (baja lógica, se conserva el registro para trazabilidad y auditoría).

## 2. Reglas de inscripción a finales

Un alumno puede inscribirse a una mesa de examen **si y sólo si**:

1. **Materia regularizada**: `estado_academico_materia.estado = REGULAR` (o `APROBADA` si la institución permite rendir para mejorar nota, configurable) para la materia de la mesa.
2. **Sin superposición de mesas**: no debe existir otra `InscripcionFinal` activa del alumno para una mesa con la misma `fecha` y `turno`.
3. **Máximo de intentos no superado**: `COUNT(intento_final WHERE alumno_id = X AND materia_id = Y) < configuracion.limiteIntentosFinal`.
4. **Antelación mínima de 48 horas**: `mesa.fecha - now() >= configuracion.horasBloqueoFinal horas`. Pasado ese umbral, la mesa deja de estar disponible para inscripción/baja/modificación.
5. **Dentro del período de inscripción a finales**: `now() BETWEEN configuracion.fechaAperturaFinales AND configuracion.fechaCierreFinales`.
6. **Sin inscripción duplicada** a la misma mesa (restricción de unicidad).

**Modificación/baja**: mismas reglas de antelación de 48 horas; pasado ese umbral el sistema bloquea cualquier cambio, aun si el usuario tiene el período de inscripción general abierto.

## 3. Reglas de configuración institucional (parámetros globales)

Administrado desde `configuracion_institucional` (fila única), editable únicamente por ADMINISTRADOR:

| Parámetro | Efecto |
|---|---|
| `fechaAperturaCursadas` / `fechaCierreCursadas` | Ventana en la que se permite inscribirse/modificar cursadas |
| `fechaAperturaFinales` / `fechaCierreFinales` | Ventana en la que se permite inscribirse/modificar finales |
| `limiteIntentosFinal` | Máximo de veces que un alumno puede inscribirse a rendir la misma materia |
| `horasBloqueoFinal` | Horas mínimas de antelación exigidas para inscripción/modificación/baja de finales |

Toda modificación de estos parámetros queda registrada en `auditoria` con el detalle de los valores anteriores y nuevos.

## 4. Catálogo completo de validaciones

### 4.1 Usuarios
- No permitir dos usuarios con el mismo **DNI** (constraint UNIQUE + validación explícita con mensaje de negocio antes de llegar al constraint).
- No permitir dos usuarios con el mismo **email**.
- Contraseña con política mínima (longitud, mayúscula, número) al alta y en cambios de contraseña.
- Un usuario debe tener al menos un rol asignado.

### 4.2 Materias y plan de estudio
- No permitir **materias duplicadas** (mismo código).
- No permitir asociar una materia dos veces al mismo plan de estudio (PK compuesta `plan_materia`).
- No permitir correlatividades **cíclicas** (A requiere B, B requiere A) — validado recorriendo el grafo de correlatividades antes de persistir.
- No permitir que una materia sea correlativa de sí misma (constraint `CHECK` + validación de servicio).

### 4.3 Horarios y comisiones
- No permitir **horarios superpuestos** dentro de la misma comisión (dos bloques del mismo día que se solapan).
- Validar que `horaFin > horaInicio`.
- No permitir cupo máximo ≤ 0.
- Alertar (a nivel de UI, y bloquear si se configura como estricto) sobre superposición de aula: dos comisiones distintas no deberían compartir aula, día y franja horaria simultáneamente.

### 4.4 Mesas de examen
- No permitir **mesas fuera de fecha** (fecha de mesa no puede ser anterior a la fecha de creación ni fuera del calendario académico vigente).
- No permitir dos mesas de la misma materia el mismo día y turno (constraint UNIQUE).

### 4.5 Inscripciones
- No permitir **inscripciones repetidas** (a la misma comisión o a la misma mesa), reforzado con constraint UNIQUE + verificación de negocio con mensaje claro.
- Validar **correlatividades automáticamente** antes de aceptar cualquier inscripción a cursada.
- Validar **cupo** antes de aceptar cualquier inscripción a cursada o final.
- Validar **estado académico** (regularidad) antes de aceptar inscripción a final.
- Validar **intentos** antes de aceptar inscripción a final.
- Validar **superposición horaria** (cursadas) y **superposición de mesa** (finales).
- Validar **ventana temporal** (apertura/cierre general, y antelación de 48hs para finales).

### 4.6 Auditoría y trazabilidad
- Toda operación de alta/baja/modificación sobre entidades sensibles genera un registro de auditoría (ver `08-auditoria.md`), incluyendo el usuario, la IP, la acción y un detalle estructurado del cambio.

## 5. Implementación como validadores componibles (Strategy + Chain of Responsibility)

```java
public interface ValidadorInscripcionCursada {
    void validar(Alumno alumno, Comision comision) throws BusinessException;
}

@Component class ValidadorCorrelatividad implements ValidadorInscripcionCursada { /* ... */ }
@Component class ValidadorSuperposicionHoraria implements ValidadorInscripcionCursada { /* ... */ }
@Component class ValidadorCupoDisponible implements ValidadorInscripcionCursada { /* ... */ }
@Component class ValidadorVentanaInscripcion implements ValidadorInscripcionCursada { /* ... */ }

@Service
public class InscripcionCursadaServiceImpl implements InscripcionCursadaService {
    private final List<ValidadorInscripcionCursada> validadores; // inyectados por Spring en orden de @Order

    @Override
    @Transactional
    public InscripcionCursadaResponse inscribir(Long alumnoId, Long comisionId) {
        Alumno alumno = alumnoRepository.findById(alumnoId)...;
        Comision comision = comisionRepository.findById(comisionId)...;
        for (ValidadorInscripcionCursada v : validadores) {
            v.validar(alumno, comision); // lanza BusinessException con código específico si falla
        }
        InscripcionCursada nueva = ...;
        InscripcionCursada guardada = inscripcionCursadaRepository.save(nueva);
        eventPublisher.publishEvent(new InscripcionRealizadaEvent(alumnoId, comisionId)); // dispara auditoría
        return mapper.toResponse(guardada);
    }
}
```

La misma estructura se replica para `InscripcionFinalServiceImpl` con sus validadores propios (`ValidadorRegularidad`, `ValidadorSuperposicionMesa`, `ValidadorIntentosMaximos`, `ValidadorAntelacionMinima`).

## 6. Siguiente etapa

Con las reglas de negocio y validaciones exhaustivamente definidas, el siguiente documento (`08-auditoria.md`) especifica el modelo completo de auditoría y trazabilidad del sistema.
