# 09. Diagramas UML

Todos los diagramas están en sintaxis **Mermaid**, renderizables en GitHub, GitLab, VS Code (extensión Mermaid) o cualquier visor compatible.

## 1. Diagrama de casos de uso

```mermaid
graph TB
    Alumno((Alumno))
    Preceptor((Preceptor))
    Administrador((Administrador))

    Alumno --> UC1[Iniciar sesión]
    Alumno --> UC2[Consultar historial académico]
    Alumno --> UC3[Inscribirse a cursada]
    Alumno --> UC4[Inscribirse a final]
    Alumno --> UC5[Modificar inscripción]
    Alumno --> UC6[Descargar certificados]

    Preceptor --> UC1
    Preceptor --> UC7[Buscar alumnos]
    Preceptor --> UC8[Consultar estado académico]
    Preceptor --> UC9[Exportar a Excel]
    Preceptor --> UC6

    Administrador --> UC1
    Administrador --> UC10[Gestionar usuarios]
    Administrador --> UC11[Gestionar oferta académica]
    Administrador --> UC12[Configurar reglas institucionales]
    Administrador --> UC13[Consultar auditoría]
```

## 2. Diagrama de clases (dominio académico, simplificado)

```mermaid
classDiagram
    class Usuario {
      +Long id
      +String dni
      +String email
      +String passwordHash
      +boolean activo
    }
    class Rol { +Long id; +String nombre }
    class Alumno { +String legajo; +LocalDate fechaIngreso }
    class Preceptor { +String legajoEmpleado }
    class Administrador
    class Carrera { +String codigo; +String nombre }
    class PlanEstudio { +String version; +boolean vigente }
    class Materia { +String codigo; +String nombre; +int cargaHoraria }
    class Correlatividad { +TipoRequisito tipoRequisito }
    class Comision { +String nombre; +String periodoLectivo; +int cupoMaximo }
    class Horario { +int diaSemana; +LocalTime horaInicio; +LocalTime horaFin }
    class InscripcionCursada { +EstadoInscripcion estado; +LocalDateTime fechaInscripcion }
    class MesaExamen { +LocalDate fecha; +Turno turno }
    class InscripcionFinal { +EstadoInscripcion estado }
    class IntentoFinal { +int nroIntento; +ResultadoFinal resultado }
    class EstadoAcademicoMateria { +EstadoMateria estado }
    class CertificadoEmitido { +TipoCertificado tipo; +String hashDocumento }
    class ConfiguracionInstitucional { +int limiteIntentosFinal; +int horasBloqueoFinal }

    Usuario "1" --> "0..*" Rol : tiene
    Usuario "1" --> "0..1" Alumno
    Usuario "1" --> "0..1" Preceptor
    Usuario "1" --> "0..1" Administrador
    Carrera "1" --> "0..*" PlanEstudio
    PlanEstudio "0..*" --> "0..*" Materia : plan_materia
    Materia "1" --> "0..*" Correlatividad
    Materia "1" --> "0..*" Comision
    Comision "1" --> "0..*" Horario
    Comision "1" --> "0..*" InscripcionCursada
    Alumno "1" --> "0..*" InscripcionCursada
    Materia "1" --> "0..*" MesaExamen
    MesaExamen "1" --> "0..*" InscripcionFinal
    Alumno "1" --> "0..*" InscripcionFinal
    InscripcionFinal "1" --> "0..*" IntentoFinal
    Alumno "1" --> "0..*" EstadoAcademicoMateria
    Materia "1" --> "0..*" EstadoAcademicoMateria
    Alumno "1" --> "0..*" CertificadoEmitido
```

## 3. Diagrama de secuencia — Inscripción a cursada

```mermaid
sequenceDiagram
    actor Alumno
    participant FE as Frontend (React)
    participant AC as InscripcionCursadaController
    participant SV as InscripcionCursadaService
    participant VAL as Validadores
    participant REPO as Repository
    participant EVT as EventPublisher

    Alumno->>FE: Selecciona comisión y confirma
    FE->>AC: POST /inscripciones-cursada {comisionId}
    AC->>SV: inscribir(alumnoId, comisionId)
    SV->>VAL: validarCorrelatividad()
    VAL-->>SV: OK
    SV->>VAL: validarSuperposicionHoraria()
    VAL-->>SV: OK
    SV->>VAL: validarCupoDisponible()
    VAL-->>SV: OK
    SV->>VAL: validarVentanaInscripcion()
    VAL-->>SV: OK
    SV->>REPO: save(InscripcionCursada)
    REPO-->>SV: entidad persistida
    SV->>EVT: publish(InscripcionRealizadaEvent)
    SV-->>AC: InscripcionCursadaResponse
    AC-->>FE: 201 Created + DTO
    FE-->>Alumno: Confirmación visual
```

## 4. Diagrama de secuencia — Inscripción a final (con rechazo por reglas)

```mermaid
sequenceDiagram
    actor Alumno
    participant FE as Frontend
    participant AC as InscripcionFinalController
    participant SV as InscripcionFinalService
    participant VAL as Validadores

    Alumno->>FE: Elige mesa de examen
    FE->>AC: POST /inscripciones-final {mesaId}
    AC->>SV: inscribir(alumnoId, mesaId)
    SV->>VAL: validarRegularidad()
    VAL-->>SV: OK
    SV->>VAL: validarIntentosMaximos()
    VAL-->>SV: Excepción: LIMITE_INTENTOS_SUPERADO
    SV-->>AC: BusinessException(422)
    AC-->>FE: 422 Unprocessable Entity + ErrorResponse
    FE-->>Alumno: Muestra alerta con motivo del rechazo
```

## 5. Diagrama Entidad-Relación (resumen)

```mermaid
erDiagram
    USUARIO ||--o{ USUARIO_ROL : tiene
    ROL ||--o{ USUARIO_ROL : asignado_a
    USUARIO ||--o| ALUMNO : es
    USUARIO ||--o| PRECEPTOR : es
    USUARIO ||--o| ADMINISTRADOR : es
    CARRERA ||--o{ PLAN_ESTUDIO : tiene
    PLAN_ESTUDIO ||--o{ PLAN_MATERIA : incluye
    MATERIA ||--o{ PLAN_MATERIA : parte_de
    MATERIA ||--o{ CORRELATIVIDAD : requiere
    MATERIA ||--o{ COMISION : dictada_en
    COMISION ||--o{ HORARIO : tiene
    ALUMNO ||--o{ INSCRIPCION_CURSADA : realiza
    COMISION ||--o{ INSCRIPCION_CURSADA : recibe
    MATERIA ||--o{ MESA_EXAMEN : ofrece
    ALUMNO ||--o{ INSCRIPCION_FINAL : realiza
    MESA_EXAMEN ||--o{ INSCRIPCION_FINAL : recibe
    INSCRIPCION_FINAL ||--o{ INTENTO_FINAL : genera
    ALUMNO ||--o{ ESTADO_ACADEMICO_MATERIA : tiene
    MATERIA ||--o{ ESTADO_ACADEMICO_MATERIA : referencia
    ALUMNO ||--o{ CERTIFICADO_EMITIDO : solicita
    USUARIO ||--o{ AUDITORIA : genera
    USUARIO ||--o{ REFRESH_TOKEN : posee
```

## 6. Diagrama de arquitectura (capas y despliegue)

```mermaid
graph TB
    subgraph Cliente
      RF[React + TS + Vite + MUI]
    end
    subgraph Backend["Spring Boot (Java 17)"]
      CTRL[Controllers]
      SVC[Services / Use Cases]
      DOM[Entidades de dominio]
      REPO[Repositories - Spring Data JPA]
      SEC[Security - JWT / Spring Security]
    end
    DB[(MySQL 8)]
    RF -->|HTTPS + JWT| CTRL
    CTRL --> SVC
    SVC --> DOM
    SVC --> REPO
    REPO --> DB
    SEC -.-> CTRL
```

## 7. Siguiente etapa

Con toda la documentación técnica y los diagramas completos, el último documento (`10-roadmap-entregables.md`) define las etapas de desarrollo, el backlog técnico y los criterios de aceptación para llevar este diseño a un sistema funcionando.
