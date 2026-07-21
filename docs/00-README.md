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

Análisis y arquitectura (Etapa 1) completos, y en desarrollo activo el backend (Etapas 2-4). Implementado y con tests (unitarios + integración) en `backend/`: autenticación JWT, ABM de usuarios, y CRUD completo de Carrera, Materia, Correlatividad (con detección de ciclos), Comisión/Horario (con detección de superposición), Mesa de Examen, Configuración Institucional y Auditoría (evento genérico, wireado en todos los servicios anteriores).

Pendiente: Plan de Estudio, el pipeline de validadores de Inscripción a Cursada/Final (Etapa 5), certificados PDF y exportación Excel, frontend completo (Etapa 6), y despliegue (Etapa 8). Ver `10-roadmap-entregables.md` para el detalle por etapa, y `../CLAUDE.md` en la raíz del repo para el estado técnico exacto (paquetes, comandos, decisiones tomadas durante la implementación) que guía a quien retome el desarrollo.
