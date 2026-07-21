# 08. Auditoría y Trazabilidad

## 1. Objetivo

Garantizar trazabilidad completa de toda acción sensible del sistema: quién la hizo, cuándo, desde dónde, y qué cambió — reemplazando la trazabilidad nula del proceso manual actual con Excel.

## 2. Eventos auditados obligatoriamente

| Evento | Disparador |
|---|---|
| Inicio de sesión (exitoso y fallido) | `AuthController.login` |
| Cierre de sesión | `AuthController.logout` |
| Alta / baja / modificación de usuarios | `UsuarioService` |
| Cambios de roles | `UsuarioService.actualizarRoles` |
| Inscripción / modificación / baja de cursada | `InscripcionCursadaService` |
| Inscripción / modificación / baja de final | `InscripcionFinalService` |
| Cambios académicos (alta/modificación de estado en `estado_academico_materia`) | `EstadoAcademicoService` |
| Altas/modificaciones/bajas de oferta académica (carrera, plan, materia, correlatividad, comisión, horario, mesa) | Services de administración |
| Cambios en configuración institucional | `ConfiguracionInstitucionalService` |
| Exportaciones a Excel | `ExportacionService` |
| Descargas de certificados PDF | `CertificadoService` |

## 3. Estructura del registro de auditoría

Cada fila de `auditoria` contiene (ver DDL en `02-modelo-datos.md`):

- `usuario_id`: quién ejecutó la acción (nullable para intentos de login fallidos sin usuario resuelto).
- `accion`: código de la acción (`LOGIN_EXITOSO`, `LOGIN_FALLIDO`, `USUARIO_CREADO`, `INSCRIPCION_CURSADA_ALTA`, `INSCRIPCION_CURSADA_BAJA`, `CONFIGURACION_MODIFICADA`, `CERTIFICADO_DESCARGADO`, etc.).
- `entidad` / `entidad_id`: tipo y ID del recurso afectado.
- `detalle`: JSON con el "antes/después" relevante (por ejemplo, para `CONFIGURACION_MODIFICADA`: `{"fechaCierreCursadas": {"antes": "...", "despues": "..."}}`).
- `ip`: dirección IP de origen del request (extraída respetando `X-Forwarded-For` detrás de proxy/balanceador).
- `fecha`: timestamp del evento.

## 4. Implementación desacoplada (Observer / eventos de dominio)

Para no acoplar cada `Service` de negocio al módulo de auditoría (principio de responsabilidad única), se usa el mecanismo de eventos de Spring:

```java
public record InscripcionRealizadaEvent(Long alumnoId, Long comisionId, String ip) {}

@Component
public class AuditoriaListener {

    @Async
    @EventListener
    public void onInscripcionRealizada(InscripcionRealizadaEvent event) {
        auditoriaService.registrar(
            event.alumnoId(), "INSCRIPCION_CURSADA_ALTA", "InscripcionCursada",
            event.comisionId(), Map.of("comisionId", event.comisionId()), event.ip());
    }
}
```

Cada `Service` de negocio simplemente publica el evento correspondiente tras confirmar la operación (`ApplicationEventPublisher.publishEvent(...)`); el listener de auditoría persiste el registro de forma asíncrona, sin bloquear la respuesta al usuario y sin que el `Service` de negocio conozca los detalles de cómo se audita.

Alternativa complementaria: un **aspecto AOP** (`@Aspect`) sobre anotaciones custom `@Auditable(accion = "...")` en los métodos de `Controller` o `Service`, para reducir código repetido de publicación de eventos en casos homogéneos (altas/bajas CRUD de administración).

## 5. Consulta de auditoría (rol Administrador)

`GET /api/v1/auditoria` soporta filtros combinables:
- `usuarioId`
- `accion`
- `entidad`
- `fechaDesde` / `fechaHasta`

Resultado paginado y ordenado por fecha descendente por defecto. El detalle JSON se muestra formateado en el frontend dentro de un panel expandible por fila (ver `05-modulos-frontend.md`, módulo Auditoría).

## 6. Retención e inmutabilidad

- Los registros de auditoría son **append-only**: no existen endpoints de modificación ni borrado, ni a nivel de API ni de permisos de base de datos para el usuario de aplicación (sólo `INSERT`/`SELECT`).
- Política de retención sugerida: mínimo el ciclo lectivo vigente más los dos anteriores, con posibilidad de archivado en frío (exportación periódica) para registros más antiguos, dado que el volumen de auditoría crece de forma continua.

## 7. Siguiente etapa

Con la auditoría especificada, el siguiente documento (`09-diagramas-uml.md`) presenta los diagramas de clases, secuencia, casos de uso y entidad-relación en formato Mermaid.
