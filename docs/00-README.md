# Sistema Web de Gestión Académica (SIGA)

Documentación técnica completa del sistema que reemplaza el proceso actual de inscripciones por Google Forms + Excel, para una institución educativa.

## Índice de documentos

| # | Documento | Contenido |
|---|-----------|-----------|
| 01 | [Análisis y Arquitectura](./01-analisis-arquitectura.md) | Contexto, alcance, arquitectura Clean Architecture, capas, stack tecnológico |
| 02 | [Modelo de Datos](./02-modelo-datos.md) | Modelo E-R, modelo relacional, DDL completo, índices, normalización, datos de prueba |
| 03 | [Entidades, DTOs y Mappers](./03-entidades-dto.md) | Todas las entidades JPA, DTOs de request/response, mappers |
| 04 | [API REST](./04-api-endpoints.md) | Catálogo completo de endpoints por módulo, contratos, códigos de error |
| 05 | [Frontend](./05-modulos-frontend.md) | Arquitectura React, rutas, dashboards, componentes, wireframes |
| 06 | [Seguridad y Autenticación](./06-seguridad-autenticacion.md) | JWT, roles, permisos, refresh token, hashing, rate limiting |
| 07 | [Reglas de Negocio y Validaciones](./07-reglas-negocio-validaciones.md) | Reglas de cursadas, finales, correlatividades, cupos, validaciones |
| 08 | [Auditoría](./08-auditoria.md) | Modelo de auditoría, eventos registrados, trazabilidad |
| 09 | [Diagramas UML](./09-diagramas-uml.md) | Diagrama de clases, secuencia, casos de uso, ER (Mermaid) |
| 10 | [Roadmap y Entregables](./10-roadmap-entregables.md) | Etapas de desarrollo, backlog técnico, criterios de aceptación |

## Cómo leer esta documentación

Cada documento es independiente pero se referencian entre sí. El orden recomendado de lectura es el de la tabla de arriba: primero arquitectura, luego datos, luego backend (entidades → API), luego frontend, y finalmente seguridad, reglas de negocio, auditoría, diagramas y roadmap.

## Estado del proyecto

Etapa actual: **Análisis completo y diseño de arquitectura** (Etapa 1 de 6). Ver `10-roadmap-entregables.md` para el detalle de todas las etapas y qué sigue después de esta entrega.
