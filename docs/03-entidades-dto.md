# 03. Entidades, DTOs y Mappers

## 1. Convenciones generales

- Todas las entidades usan `@Entity`, Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`) y auditoría de fechas vía `@EntityListeners(AuditingEntityListener.class)` con `@CreatedDate` / `@LastModifiedDate` donde aplique.
- Los IDs son `Long` con `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`, salvo herencia 1:1 (`alumno`, `preceptor`, `administrador`) que usan `@MapsId` sobre `usuario`.
- Los DTOs de **request** no incluyen campos de auditoría ni IDs generados por el sistema. Los DTOs de **response** sí incluyen IDs y timestamps de sólo lectura.
- Los mappers se generan con **MapStruct** (`@Mapper(componentModel = "spring")`), evitando lógica manual de mapeo en los services.

## 2. Entidades principales (resumen de campos)

### Usuario
```java
@Entity @Table(name = "usuario")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false, unique = true) String dni;
    @Column(nullable = false, unique = true) String email;
    @Column(name = "password_hash", nullable = false) String passwordHash;
    String nombre;
    String apellido;
    boolean activo;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_rol",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id"))
    Set<Rol> roles;
    @CreatedDate LocalDateTime fechaCreacion;
    @LastModifiedDate LocalDateTime fechaActualizacion;
}
```

### Rol
```java
@Entity @Table(name = "rol")
public class Rol {
    @Id @GeneratedValue Long id;
    @Column(unique = true) String nombre; // ALUMNO, PRECEPTOR, ADMINISTRADOR
    String descripcion;
}
```

### Alumno (herencia 1:1 con Usuario)
```java
@Entity @Table(name = "alumno")
public class Alumno {
    @Id Long usuarioId;
    @OneToOne @MapsId @JoinColumn(name = "usuario_id") Usuario usuario;
    @Column(unique = true) String legajo;
    LocalDate fechaIngreso;
    @OneToMany(mappedBy = "alumno") List<InscripcionCursada> inscripcionesCursada;
    @OneToMany(mappedBy = "alumno") List<InscripcionFinal> inscripcionesFinal;
    @OneToMany(mappedBy = "alumno") List<EstadoAcademicoMateria> estadosAcademicos;
}
```

### Preceptor / Administrador
Análogas a `Alumno`, con clave `usuarioId` mapeada 1:1 a `Usuario`. `Preceptor` agrega `legajoEmpleado`.

### Carrera / PlanEstudio / Materia / PlanMateria
```java
@Entity class Carrera { Long id; String codigo; String nombre; boolean activa;
    @OneToMany(mappedBy = "carrera") List<PlanEstudio> planes; }

@Entity class PlanEstudio { Long id;
    @ManyToOne @JoinColumn(name = "carrera_id") Carrera carrera;
    String version; boolean vigente;
    @OneToMany(mappedBy = "plan") List<PlanMateria> materias; }

@Entity class Materia { Long id; String codigo; String nombre; int cargaHoraria; boolean activa;
    @OneToMany(mappedBy = "materia") List<Comision> comisiones;
    @OneToMany(mappedBy = "materia") List<MesaExamen> mesasExamen;
    @OneToMany(mappedBy = "materia") List<Correlatividad> correlatividades; }

@Entity @IdClass(PlanMateriaId.class)
class PlanMateria {
    @Id @ManyToOne PlanEstudio plan;
    @Id @ManyToOne Materia materia;
    int anio; int cuatrimestre;
}
```

### Correlatividad
```java
@Entity class Correlatividad {
    Long id;
    @ManyToOne Materia materia;
    @ManyToOne @JoinColumn(name = "materia_requerida_id") Materia materiaRequerida;
    @Enumerated(EnumType.STRING) TipoRequisito tipoRequisito; // CURSAR, APROBAR
}
enum TipoRequisito { CURSAR, APROBAR }
```

### Comision / Horario
```java
@Entity class Comision { Long id;
    @ManyToOne Materia materia; String nombre; String periodoLectivo; int cupoMaximo;
    @OneToMany(mappedBy = "comision") List<Horario> horarios;
    @OneToMany(mappedBy = "comision") List<InscripcionCursada> inscripciones; }

@Entity class Horario { Long id;
    @ManyToOne Comision comision; int diaSemana; LocalTime horaInicio; LocalTime horaFin; String aula; }
```

### InscripcionCursada
```java
@Entity class InscripcionCursada {
    Long id;
    @ManyToOne Alumno alumno;
    @ManyToOne Comision comision;
    @Enumerated(EnumType.STRING) EstadoInscripcion estado; // INSCRIPTO, MODIFICADA, BAJA
    LocalDateTime fechaInscripcion;
    LocalDateTime fechaModificacion;
}
enum EstadoInscripcion { INSCRIPTO, MODIFICADA, BAJA }
```

### MesaExamen / InscripcionFinal / IntentoFinal
```java
@Entity class MesaExamen { Long id;
    @ManyToOne Materia materia; LocalDate fecha;
    @Enumerated(EnumType.STRING) Turno turno; int cupoMaximo;
    @OneToMany(mappedBy = "mesa") List<InscripcionFinal> inscripciones; }
enum Turno { MANANA, TARDE, NOCHE }

@Entity class InscripcionFinal { Long id;
    @ManyToOne Alumno alumno;
    @ManyToOne MesaExamen mesa;
    @Enumerated(EnumType.STRING) EstadoInscripcion estado;
    LocalDateTime fechaInscripcion; LocalDateTime fechaModificacion; }

@Entity class IntentoFinal { Long id;
    @ManyToOne Alumno alumno;
    @ManyToOne Materia materia;
    @ManyToOne InscripcionFinal inscripcionFinal;
    int nroIntento;
    @Enumerated(EnumType.STRING) ResultadoFinal resultado; // AUSENTE, DESAPROBADO, APROBADO
    LocalDate fecha; }
```

### EstadoAcademicoMateria / CertificadoEmitido
```java
@Entity class EstadoAcademicoMateria { Long id;
    @ManyToOne Alumno alumno;
    @ManyToOne Materia materia;
    @Enumerated(EnumType.STRING) EstadoMateria estado; // PENDIENTE, CURSANDO, REGULAR, APROBADA, LIBRE
    LocalDateTime fechaActualizacion; }

@Entity class CertificadoEmitido { Long id;
    @ManyToOne Alumno alumno;
    @Enumerated(EnumType.STRING) TipoCertificado tipo; // ALUMNO_REGULAR, ANALITICO_PARCIAL, COMPROBANTE_CURSADA, COMPROBANTE_FINAL
    LocalDateTime fechaEmision; String hashDocumento; }
```

### EventoAcademico (calendario — actualización post-MVP)
```java
@Entity @Table(name = "evento_academico")
public class EventoAcademico {
    @Id @GeneratedValue Long id;
    String titulo; String descripcion;
    @Enumerated(EnumType.STRING) TipoEventoAcademico tipo; // EVENTO_ACADEMICO, FECHA_LIMITE
    LocalDateTime fechaInicio; LocalDateTime fechaFin;
    @ManyToOne Usuario creadoPor;
    LocalDateTime fechaCreacion;
}
```
Solo cubre eventos institucionales sueltos y fechas límite cargadas a mano por el administrador. Cursadas, finales e inscripciones **no** se duplican acá: `CalendarioService` los deriva en tiempo real de `horario`/`comision`, `mesa_examen`, `inscripcion_cursada`/`inscripcion_final` y `configuracion_institucional` (ver `04-api-endpoints.md` §16). `comision` y `mesa_examen` ganaron además una columna `profesor VARCHAR(150)` nullable (texto libre, no hay entidad `Profesor`).

### ConfiguracionInstitucional / Auditoria / RefreshToken
```java
@Entity class ConfiguracionInstitucional {
    @Id Integer id = 1;
    LocalDateTime fechaAperturaCursadas, fechaCierreCursadas;
    LocalDateTime fechaAperturaFinales, fechaCierreFinales;
    int limiteIntentosFinal; int horasBloqueoFinal;
}

@Entity class Auditoria { Long id;
    @ManyToOne Usuario usuario; String accion; String entidad; Long entidadId;
    @Type(JsonType.class) Map<String,Object> detalle; String ip; LocalDateTime fecha; }

@Entity class RefreshToken { Long id;
    @ManyToOne Usuario usuario; String token; LocalDateTime expiracion; boolean revocado; }
```

## 3. DTOs por módulo

### Autenticación
```java
record LoginRequest(@NotBlank String dni, @NotBlank String password) {}
record LoginResponse(String accessToken, String refreshToken, String rol, String nombre) {}
record RefreshTokenRequest(@NotBlank String refreshToken) {}
```

### Usuarios
```java
record UsuarioRequest(@NotBlank String dni, @Email String email, @NotBlank String nombre,
                       @NotBlank String apellido, @NotBlank String password, Set<String> roles) {}
record UsuarioResponse(Long id, String dni, String email, String nombre, String apellido,
                        boolean activo, Set<String> roles, LocalDateTime fechaCreacion) {}
record UsuarioUpdateRequest(String email, String nombre, String apellido, Boolean activo) {}
```

### Académico (Carrera / Materia / Correlatividad / Comisión / Horario)
```java
record CarreraRequest(@NotBlank String codigo, @NotBlank String nombre) {}
record CarreraResponse(Long id, String codigo, String nombre, boolean activa) {}

record MateriaRequest(@NotBlank String codigo, @NotBlank String nombre, @Min(1) int cargaHoraria) {}
record MateriaResponse(Long id, String codigo, String nombre, int cargaHoraria, boolean activa) {}

record CorrelatividadRequest(@NotNull Long materiaId, @NotNull Long materiaRequeridaId,
                              @NotNull TipoRequisito tipoRequisito) {}

record ComisionRequest(@NotNull Long materiaId, @NotBlank String nombre,
                        @NotBlank String periodoLectivo, @Min(1) int cupoMaximo, String profesor,
                        List<HorarioRequest> horarios) {}
record HorarioRequest(@Min(1) @Max(7) int diaSemana, @NotNull LocalTime horaInicio,
                       @NotNull LocalTime horaFin, String aula) {}
```

### Inscripciones
```java
record InscripcionCursadaRequest(@NotNull Long comisionId) {}
record InscripcionCursadaResponse(Long id, String materiaNombre, String comisionNombre,
                                   String estado, LocalDateTime fechaInscripcion,
                                   String profesor, String carreraNombre, List<HorarioResponse> horarios) {}

record InscripcionFinalRequest(@NotNull Long mesaId) {}
record InscripcionFinalResponse(Long id, String materiaNombre, LocalDate fechaMesa, String turno,
                                 String estado, String profesor, String carreraNombre, String resultado) {}
```
`carreraNombre` se resuelve por `Materia → PlanMateria → PlanEstudio → Carrera` (preferido el plan vigente) — `Alumno` no tiene una carrera propia mapeada en JPA. `resultado` viene de `IntentoFinal.resultado` (no de la inscripción), buscado por `inscripcionFinalId`.

### Calendario (actualización post-MVP, `/api/v1/calendario`)
```java
record EventoCalendarioResponse(String id, String titulo, String detalle, LocalDateTime inicio,
                                 LocalDateTime fin, TipoEventoCalendario tipo, String ubicacion) {}
// TipoEventoCalendario: CURSADA, FINAL, INSCRIPCION, EVENTO_ACADEMICO, FECHA_LIMITE

record EventoAcademicoRequest(@NotBlank String titulo, String descripcion,
                               @NotNull TipoEventoAcademico tipo, @NotNull LocalDateTime fechaInicio,
                               LocalDateTime fechaFin) {}
record EventoAcademicoResponse(Long id, String titulo, String descripcion, String tipo,
                                LocalDateTime fechaInicio, LocalDateTime fechaFin) {}
```
`EventoCalendarioResponse.id` es `String` (no un id de fila): los eventos derivados llevan ids sintéticos (`"horario-12-2026-04-06"`, `"mesa-3"`) porque no existen como filas propias.

### Historial académico / Certificados
```java
record HistorialAcademicoResponse(String materiaCodigo, String materiaNombre, String estado,
                                   LocalDateTime fechaActualizacion) {}
record CertificadoResponse(Long id, String tipo, LocalDateTime fechaEmision, String urlDescarga) {}
```

### Configuración institucional
```java
record ConfiguracionInstitucionalRequest(LocalDateTime fechaAperturaCursadas, LocalDateTime fechaCierreCursadas,
                                          LocalDateTime fechaAperturaFinales, LocalDateTime fechaCierreFinales,
                                          @Min(1) int limiteIntentosFinal, @Min(1) int horasBloqueoFinal) {}
```

### Errores (respuesta estándar)
```java
record ErrorResponse(String codigo, String mensaje, LocalDateTime timestamp, List<String> detalles) {}
```

## 4. Mappers (MapStruct)

```java
@Mapper(componentModel = "spring")
public interface UsuarioMapper {
    @Mapping(target = "roles", expression = "java(mapRoles(entity.getRoles()))")
    UsuarioResponse toResponse(Usuario entity);
    Usuario toEntity(UsuarioRequest request);
    default Set<String> mapRoles(Set<Rol> roles) {
        return roles.stream().map(Rol::getNombre).collect(Collectors.toSet());
    }
}

@Mapper(componentModel = "spring")
public interface InscripcionCursadaMapper {
    @Mapping(source = "comision.materia.nombre", target = "materiaNombre")
    @Mapping(source = "comision.nombre", target = "comisionNombre")
    InscripcionCursadaResponse toResponse(InscripcionCursada entity);
}

@Mapper(componentModel = "spring")
public interface MateriaMapper {
    MateriaResponse toResponse(Materia entity);
    Materia toEntity(MateriaRequest request);
}
```

Mappers análogos existen para `CarreraMapper`, `ComisionMapper`, `HorarioMapper`, `MesaExamenMapper`, `InscripcionFinalMapper`, `EstadoAcademicoMapper`, `CertificadoMapper` y `ConfiguracionInstitucionalMapper`, todos siguiendo el mismo patrón: mapeo declarativo, sin lógica de negocio.

## 5. Siguiente etapa

Con entidades, DTOs y mappers definidos, el siguiente documento (`04-api-endpoints.md`) especifica el catálogo completo de endpoints REST expuestos por cada controller.
