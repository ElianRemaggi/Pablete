package com.instituto.siga.excel;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.response.ImportacionHistorialResponse;
import com.instituto.siga.dto.response.ImportacionHistorialResponse.Cambio;
import com.instituto.siga.dto.response.ImportacionHistorialResponse.Error;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.EstadoAcademicoMateria;
import com.instituto.siga.entity.EstadoMateria;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.Rol;
import com.instituto.siga.entity.Usuario;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.EstadoAcademicoMateriaRepository;
import com.instituto.siga.repository.MateriaRepository;
import com.instituto.siga.repository.RolRepository;
import com.instituto.siga.repository.UsuarioRepository;
import com.instituto.siga.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Importacion masiva del historial academico desde un .xlsx subido por el personal (vista previa + confirmacion).
 * Misma estructura de archivo que HistorialExcelSyncService (docs/11): una hoja por carrera/plan, DNI + datos del
 * alumno y, por materia, las columnas CURSADA / FECHA / FINAL.
 *
 * <h3>Politica de discrepancias (Excel vs base)</h3>
 * La base es la fuente operativa: el Excel solo <b>completa o avanza</b>, nunca retrocede ni pisa.
 * <ul>
 *   <li>No existe el registro (alumno, materia): se CREA con lo que dice el Excel.</li>
 *   <li>Existe y el Excel trae un estado de mayor avance (PENDIENTE/LIBRE &lt; CURSANDO &lt; REGULAR &lt; APROBADA): se ACTUALIZA
 *       (se muestra en la vista previa antes de confirmar).</li>
 *   <li>Mismo estado: si la base no tiene nota y el Excel si (APROBADA), se completa; si ambas tienen nota y difieren,
 *       es CONFLICTO.</li>
 *   <li>El Excel trae un estado de menor avance o contradictorio: CONFLICTO, se conserva la base y se informa.
 *       "PENDIENTE" en el Excel no aporta dato y no cuenta como conflicto.</li>
 *   <li>Nunca se borra nada: lo que no figura en un archivo parcial queda intacto.</li>
 *   <li>Reimportar el mismo archivo no crea duplicados: la segunda vez todo queda "sin cambios".</li>
 * </ul>
 * Los rechazos son por fila/celda y no abortan el resto; la escritura es una sola transaccion (todo o nada ante un
 * error inesperado). La nota del final solo se importa junto con el estado APROBADA.
 */
@Service
@RequiredArgsConstructor
public class HistorialExcelImportService {

    private static final int MAX_CAMBIOS_LISTADOS = 500;
    private static final int MAX_ADVERTENCIAS = 100;
    private static final Pattern DNI_VALIDO = Pattern.compile("^\\d{6,10}$");

    private final HistorialExcelReader reader;
    private final AlumnoRepository alumnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final MateriaRepository materiaRepository;
    private final EstadoAcademicoMateriaRepository estadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaPublisher auditoriaPublisher;

    /** Estado (simulado o real) de un par alumno-materia mientras se procesa el archivo. */
    private static final class Registro {
        EstadoMateria estado;
        Integer nota;
        EstadoAcademicoMateria entidad; // null si todavia no existe en la base
    }

    @PreAuthorize("hasAnyRole('PRECEPTOR', 'ADMINISTRADOR')")
    @Transactional
    public ImportacionHistorialResponse procesar(String nombreArchivo, byte[] contenido, boolean confirmar,
                                                 boolean crearAlumnos) {
        if (contenido == null || contenido.length == 0) {
            throw new BusinessException("ARCHIVO_INVALIDO", "El archivo está vacío");
        }
        HistorialExcelReader.Lectura lectura;
        try {
            lectura = reader.leer(new ByteArrayInputStream(contenido));
        } catch (Exception e) {
            throw new BusinessException("ARCHIVO_INVALIDO",
                    "No se pudo leer el archivo: debe ser un libro de Excel (.xlsx) válido", List.of(String.valueOf(e.getMessage())));
        }
        if (lectura.filas().isEmpty()) {
            List<String> detalles = new ArrayList<>(lectura.problemasDeEstructura());
            detalles.add("Columnas obligatorias de cada hoja: DNI, APELLIDO/S, NOMBRE/S, COHORTE y, por materia, "
                    + "\"N - MATERIA CURSADA\", \"N - MATERIA FECHA\", \"N - MATERIA FINAL\" (encabezados en la fila 1, alumnos desde la fila 3)");
            throw new BusinessException("ARCHIVO_SIN_DATOS",
                    "El archivo no contiene alumnos para importar (hojas vacías o sin las columnas esperadas)", detalles);
        }

        List<Error> errores = new ArrayList<>();
        List<String> advertencias = new ArrayList<>(lectura.problemasDeEstructura());
        List<Cambio> cambios = new ArrayList<>();
        boolean[] recortado = {false};
        int[] cont = new int[6]; // 0 creados 1 actualizados 2 sinCambios 3 conflictos 4 rechazadosCelda 5 filasRechazadas

        Map<String, Alumno> alumnosPorDni = new HashMap<>();
        Set<String> legajos = new HashSet<>();
        for (Alumno a : alumnoRepository.findAll()) {
            alumnosPorDni.put(a.getUsuario().getDni(), a);
            legajos.add(a.getLegajo());
        }
        Set<String> dniUsuarios = new HashSet<>();
        Set<String> emails = new HashSet<>();
        for (Usuario u : usuarioRepository.findAll()) {
            dniUsuarios.add(u.getDni());
            emails.add(u.getEmail());
        }
        Map<String, List<Materia>> materiasPorNombre = new HashMap<>();
        for (Materia m : materiaRepository.findAll()) {
            materiasPorNombre.computeIfAbsent(claveMateria(m.getNombre()), k -> new ArrayList<>()).add(m);
        }

        Map<String, String[]> aCrear = new LinkedHashMap<>(); // dni -> {apellido, nombre, cohorte}
        if (crearAlumnos) {
            for (HistorialExcelRow fila : lectura.filas()) {
                String dni = normalizarDni(fila.dni());
                if (DNI_VALIDO.matcher(dni).matches() && !alumnosPorDni.containsKey(dni) && !dniUsuarios.contains(dni)
                        && !fila.apellido().isBlank() && !fila.nombre().isBlank()) {
                    aCrear.putIfAbsent(dni, new String[]{fila.apellido(), fila.nombre(), fila.cohorte()});
                }
            }
        }
        // BCrypt (costo 12) es lento: las claves iniciales de los alumnos nuevos se calculan en paralelo, y solo al confirmar.
        Map<String, String> hashes = new HashMap<>();
        if (confirmar && crearAlumnos && !aCrear.isEmpty()) {
            aCrear.keySet().parallelStream().forEach(dni -> {
                String hash = passwordEncoder.encode(dni);
                synchronized (hashes) {
                    hashes.put(dni, hash);
                }
            });
        }
        Rol rolAlumno = confirmar && crearAlumnos ? rolRepository.findByNombre("ALUMNO").orElseThrow(
                () -> new BusinessException("ROL_NO_ENCONTRADO", "No existe el rol ALUMNO")) : null;

        Map<String, Registro> registros = new HashMap<>();
        Set<String> alumnoCargado = new HashSet<>();
        Set<String> hojaDni = new HashSet<>();
        Set<String> hojas = new HashSet<>();
        Set<String> nuevosContados = new HashSet<>();
        Set<String> existentesContados = new HashSet<>();

        for (HistorialExcelRow fila : lectura.filas()) {
            hojas.add(fila.hoja());
            String dni = normalizarDni(fila.dni());
            if (!DNI_VALIDO.matcher(dni).matches()) {
                errores.add(new Error(fila.hoja(), fila.dni(), null, "DNI inválido (deben ser entre 6 y 10 dígitos)"));
                cont[5]++;
                continue;
            }
            if (!hojaDni.add(fila.hoja() + "|" + dni)) {
                errores.add(new Error(fila.hoja(), dni, null, "DNI duplicado en la misma hoja: se ignora esta fila y se usa la primera"));
                cont[5]++;
                continue;
            }

            Alumno alumno = alumnosPorDni.get(dni);
            boolean nuevo = false;
            if (alumno == null) {
                if (!crearAlumnos) {
                    errores.add(new Error(fila.hoja(), dni, null,
                            "El alumno no existe en SIGA (activá \"Crear alumnos que no existan\" para darlo de alta)"));
                    cont[5]++;
                    continue;
                }
                if (dniUsuarios.contains(dni)) {
                    errores.add(new Error(fila.hoja(), dni, null, "El DNI pertenece a un usuario que no es alumno"));
                    cont[5]++;
                    continue;
                }
                if (fila.apellido().isBlank() || fila.nombre().isBlank()) {
                    errores.add(new Error(fila.hoja(), dni, null, "Faltan apellido o nombre: no se puede crear el alumno"));
                    cont[5]++;
                    continue;
                }
                nuevo = true;
                if (confirmar) {
                    alumno = crearAlumno(dni, fila, hashes.get(dni), rolAlumno, legajos, emails);
                    alumnosPorDni.put(dni, alumno);
                    dniUsuarios.add(dni);
                }
            }
            if (nuevo) {
                nuevosContados.add(dni);
            } else if (!nuevosContados.contains(dni)) {
                existentesContados.add(dni);
            }

            if (!nuevo && alumnoCargado.add(dni)) {
                for (EstadoAcademicoMateria e : estadoRepository.findByAlumnoId(alumno.getUsuarioId())) {
                    Registro r = new Registro();
                    r.estado = e.getEstado();
                    r.nota = e.getNota();
                    r.entidad = e;
                    registros.put(dni + ":" + e.getMateria().getId(), r);
                }
            }

            String nombreAlumno = fila.apellido() + ", " + fila.nombre();
            for (HistorialExcelMateriaCelda celda : fila.materias()) {
                procesarCelda(fila, dni, nombreAlumno, celda, alumno, nuevo, confirmar, materiasPorNombre, registros,
                        errores, advertencias, cambios, recortado, cont);
            }
        }

        ImportacionHistorialResponse respuesta = new ImportacionHistorialResponse(confirmar, nombreArchivo, hojas.size(),
                lectura.filas().size(), existentesContados.size(), nuevosContados.size(), cont[0], cont[1], cont[2], cont[3],
                cont[4], cont[5], cambios, recortado[0], errores, advertencias);

        if (confirmar) {
            Map<String, Object> detalle = new HashMap<>();
            detalle.put("archivo", nombreArchivo);
            detalle.put("alumnosNuevos", nuevosContados.size());
            detalle.put("registrosCreados", cont[0]);
            detalle.put("registrosActualizados", cont[1]);
            detalle.put("registrosEnConflicto", cont[3]);
            detalle.put("registrosRechazados", cont[4] + cont[5]);
            auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "HISTORIAL_EXCEL_IMPORTADO",
                    "EstadoAcademicoMateria", null, detalle);
        }
        return respuesta;
    }

    private void procesarCelda(HistorialExcelRow fila, String dni, String nombreAlumno, HistorialExcelMateriaCelda celda,
                               Alumno alumno, boolean alumnoNuevo, boolean confirmar,
                               Map<String, List<Materia>> materiasPorNombre, Map<String, Registro> registros,
                               List<Error> errores, List<String> advertencias, List<Cambio> cambios, boolean[] recortado,
                               int[] cont) {
        List<Materia> candidatas = materiasPorNombre.getOrDefault(claveMateria(celda.nombreMateria()), List.of());
        if (candidatas.size() != 1) {
            rechazarCelda(errores, cont, fila, dni, celda.nombreMateria(), candidatas.isEmpty()
                    ? "La materia no existe en SIGA" : "El nombre de la materia es ambiguo (coincide con varias materias)");
            return;
        }
        Materia materia = candidatas.get(0);

        EstadoMateria estado = null;
        if (!celda.estadoTexto().isBlank() && !celda.estadoTexto().trim().matches("-+")) {
            try {
                estado = EstadoMateria.valueOf(celda.estadoTexto().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                rechazarCelda(errores, cont, fila, dni, materia.getNombre(),
                        "Estado \"" + celda.estadoTexto().trim() + "\" no reconocido (válidos: PENDIENTE, CURSANDO, REGULAR, APROBADA, LIBRE)");
                return;
            }
        }
        if (celda.notaTexto() != null) {
            rechazarCelda(errores, cont, fila, dni, materia.getNombre(), "Nota inválida: \"" + celda.notaTexto() + "\" no es un número");
            return;
        }
        Integer nota = celda.nota();
        if (nota != null && (nota < 1 || nota > 10)) {
            rechazarCelda(errores, cont, fila, dni, materia.getNombre(), "Nota inválida: " + nota + " (debe estar entre 1 y 10)");
            return;
        }
        if (estado == null && nota == null) {
            return; // celda sin dato util ("-----"): no aporta ni se cuenta
        }
        if (estado == null) {
            rechazarCelda(errores, cont, fila, dni, materia.getNombre(), "Tiene nota pero no estado de la materia");
            return;
        }
        if (nota != null && estado != EstadoMateria.APROBADA) {
            if (advertencias.size() < MAX_ADVERTENCIAS) {
                advertencias.add("DNI " + dni + " / " + materia.getNombre() + ": la nota " + nota
                        + " se ignora porque el estado es " + estado + " (la nota del final se importa solo con APROBADA)");
            }
            nota = null;
        }

        String clave = dni + ":" + materia.getId();
        Registro actual = registros.get(clave);
        LocalDateTime fechaEstado = estado == EstadoMateria.APROBADA && celda.fecha() != null
                ? celda.fecha().atStartOfDay() : LocalDateTime.now();

        if (actual == null) {
            registrarCambio(cambios, recortado, new Cambio(fila.hoja(), dni, nombreAlumno, materia.getNombre(), "CREAR",
                    null, estado.name(), null, nota, alumnoNuevo ? "Alumno nuevo" : null));
            cont[0]++;
            Registro nuevo = new Registro();
            nuevo.estado = estado;
            nuevo.nota = nota;
            if (confirmar) {
                nuevo.entidad = estadoRepository.save(EstadoAcademicoMateria.builder().alumno(alumno).materia(materia)
                        .estado(estado).nota(nota).fechaActualizacion(fechaEstado).build());
            }
            registros.put(clave, nuevo);
            return;
        }

        Decision decision = decidir(actual, estado, nota);
        switch (decision.tipo) {
            case SIN_CAMBIOS -> cont[2]++;
            case CONFLICTO -> {
                cont[3]++;
                registrarCambio(cambios, recortado, new Cambio(fila.hoja(), dni, nombreAlumno, materia.getNombre(), "CONFLICTO",
                        actual.estado.name(), estado.name(), actual.nota, nota, decision.detalle + "; se conserva lo que ya hay en SIGA"));
            }
            case ACTUALIZAR -> {
                cont[1]++;
                registrarCambio(cambios, recortado, new Cambio(fila.hoja(), dni, nombreAlumno, materia.getNombre(), "ACTUALIZAR",
                        actual.estado.name(), decision.estadoNuevo.name(), actual.nota, decision.notaNueva, decision.detalle));
                if (confirmar && actual.entidad != null) {
                    actual.entidad.setEstado(decision.estadoNuevo);
                    actual.entidad.setNota(decision.notaNueva);
                    actual.entidad.setFechaActualizacion(fechaEstado);
                    estadoRepository.save(actual.entidad);
                }
                actual.estado = decision.estadoNuevo;
                actual.nota = decision.notaNueva;
            }
        }
    }

    private enum Tipo { SIN_CAMBIOS, ACTUALIZAR, CONFLICTO }

    private record Decision(Tipo tipo, EstadoMateria estadoNuevo, Integer notaNueva, String detalle) {
    }

    private Decision decidir(Registro db, EstadoMateria excel, Integer notaExcel) {
        if (excel == db.estado) {
            if (notaExcel == null || Objects.equals(notaExcel, db.nota)) {
                return new Decision(Tipo.SIN_CAMBIOS, db.estado, db.nota, null);
            }
            if (db.nota == null) {
                return new Decision(Tipo.ACTUALIZAR, db.estado, notaExcel, "Se completa la nota del final");
            }
            return new Decision(Tipo.CONFLICTO, db.estado, db.nota, "La nota del Excel (" + notaExcel + ") difiere de la registrada (" + db.nota + ")");
        }
        if (excel == EstadoMateria.PENDIENTE) {
            return new Decision(Tipo.SIN_CAMBIOS, db.estado, db.nota, null); // "pendiente" no aporta dato
        }
        boolean avanza = rango(excel) > rango(db.estado) || (excel == EstadoMateria.LIBRE && db.estado == EstadoMateria.PENDIENTE);
        if (avanza) {
            Integer nota = excel == EstadoMateria.APROBADA ? (notaExcel != null ? notaExcel : db.nota) : db.nota;
            return new Decision(Tipo.ACTUALIZAR, excel, nota, "El Excel trae un estado de mayor avance (" + db.estado + " → " + excel + ")");
        }
        return new Decision(Tipo.CONFLICTO, db.estado, db.nota, "El Excel dice " + excel + " y SIGA tiene " + db.estado);
    }

    private int rango(EstadoMateria e) {
        return switch (e) {
            case PENDIENTE, LIBRE -> 0;
            case CURSANDO -> 1;
            case REGULAR -> 2;
            case APROBADA -> 3;
        };
    }

    private void rechazarCelda(List<Error> errores, int[] cont, HistorialExcelRow fila, String dni, String materia, String motivo) {
        errores.add(new Error(fila.hoja(), dni, materia, motivo));
        cont[4]++;
    }

    private void registrarCambio(List<Cambio> cambios, boolean[] recortado, Cambio cambio) {
        if (cambios.size() < MAX_CAMBIOS_LISTADOS) {
            cambios.add(cambio);
        } else {
            recortado[0] = true;
        }
    }

    private Alumno crearAlumno(String dni, HistorialExcelRow fila, String hash, Rol rol, Set<String> legajos, Set<String> emails) {
        String legajo = legajos.contains(dni) ? "IMP-" + dni : dni;
        String email = dni + "@sin-email.siga.local";
        if (emails.contains(email)) {
            email = dni + "+imp@sin-email.siga.local";
        }
        Usuario usuario = usuarioRepository.save(Usuario.builder().dni(dni).email(email).passwordHash(hash)
                .nombre(fila.nombre()).apellido(fila.apellido()).activo(true)
                .roles(new HashSet<>(Set.of(rol))).build());
        legajos.add(legajo);
        emails.add(email);
        return alumnoRepository.save(Alumno.builder().usuario(usuario).legajo(legajo).fechaIngreso(fechaIngreso(fila.cohorte())).build());
    }

    private LocalDate fechaIngreso(String cohorte) {
        try {
            int anio = Integer.parseInt(cohorte.trim());
            if (anio >= 1990 && anio <= LocalDate.now().getYear() + 1) {
                return LocalDate.of(anio, 3, 1);
            }
        } catch (NumberFormatException ignorado) {
            // cohorte vacia o no numerica: se usa la fecha de hoy
        }
        return LocalDate.now();
    }

    /** Los encabezados del Excel mezclan "BIOLOGIA" y "BIOLOGÍA": se comparan sin tildes, sin distinguir mayusculas. */
    private String claveMateria(String nombre) {
        String sinTildes = java.text.Normalizer.normalize(nombre == null ? "" : nombre, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return sinTildes.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private String normalizarDni(String dni) {
        return dni == null ? "" : dni.replaceAll("[\\s.]", "");
    }
}
