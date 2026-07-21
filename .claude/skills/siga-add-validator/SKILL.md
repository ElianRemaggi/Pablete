---
name: siga-add-validator
description: >
  Add or change a business rule for cursada or final-exam enrollment,
  following the Strategy + Chain of Responsibility validator pattern in
  docs/07-reglas-negocio-validaciones.md. Use when asked to add an
  enrollment rule, a ValidadorInscripcionCursada / ValidadorInscripcionFinal,
  a new rejection reason/error code for inscripciones, or to change a
  threshold like cupo, correlatividades, superposición horaria, límite de
  intentos, or antelación de 48hs.
license: MIT
---

# SIGA — add an enrollment validator

Read `docs/07-reglas-negocio-validaciones.md` §1/§2/§5 first — the full
ordered rule list and the reference `InscripcionCursadaServiceImpl` shape
are there. Don't reimplement rules that already exist as a validator; check
`validation/` (cursada) or the equivalent finales validators before adding.

## Shape

```java
@Component
@Order(<next available>)
public class Validador<Regla> implements ValidadorInscripcionCursada { // or ...Final
    @Override
    public void validar(Alumno alumno, Comision comision) throws BusinessException {
        // read thresholds from ConfiguracionInstitucional, never hardcode a date/number
        // throw new BusinessException("<ERROR_CODE>", "<mensaje>") on violation
    }
}
```

## Rules

- **One validator, one rule.** Don't fold a new condition into an existing validator class — add a sibling `@Component` and let Spring inject it into the ordered list (matches Open/Closed in `docs/01` §8).
- **Every threshold comes from `configuracion_institucional`** (fechas de apertura/cierre, `limiteIntentosFinal`, `horasBloqueoFinal`) — never a hardcoded constant, per `docs/07` §3 ("reglamento puede cambiar a mitad de ciclo").
- **Error code is stable and cataloged.** Use the exact `UPPER_SNAKE_CASE` code from `docs/10-roadmap-entregables.md` §3 acceptance criteria if one already exists for this rule (`REGLA_NEGOCIO_CORRELATIVIDAD`, `CUPO_AGOTADO`, `LIMITE_INTENTOS_SUPERADO`, `FUERA_DE_PLAZO`); if the rule is new, add the code to `docs/04-api-endpoints.md` §15's error catalog in the same change, don't leave it undocumented.
- **Modification and baja re-run the same validators** against the new target (cursada) or respect the same 48h cutoff (finales) — don't special-case the update/delete path with looser checks than creation.
- **Baja is a soft delete** (`estado = BAJA`), never a row delete — the docs require the record survive for audit.
- **Publish the audit event** (`InscripcionRealizadaEvent` or the matching event for finales/modificación/baja) from the service after the validator chain passes and the row is persisted — the validator itself never touches the audit module.
- **Test at the service level**: one unit test per validator (rule holds / rule violated with the specific error code), independent of the others in the chain.
