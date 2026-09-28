package com.instituto.siga.excel;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.response.HistorialImportResultResponse;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.EstadoAcademicoMateria;
import com.instituto.siga.entity.EstadoMateria;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.EstadoAcademicoMateriaRepository;
import com.instituto.siga.repository.MateriaRepository;
import com.instituto.siga.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Orquesta la sincronizacion .xlsx -> base de datos: lee el historial academico desde el Excel
 * configurado y hace upsert de EstadoAcademicoMateria para cada (alumno, materia) que reconoce.
 *
 * Reglas explicitas (documentadas en el diseno, no improvisadas ac�):
 * - Un DNI del Excel que no corresponde a ningun Alumno ya existente en el sistema NO se importa
 *   (no se crea un Usuario/Alumno nuevo: el archivo no trae email/legajo, que son obligatorios).
 * - Un nombre de materia que no matchea (exacto, sin el prefijo ordinal, case-insensitive) ninguna
 *   Materia ya cargada NO se importa (no se crea una Materia nueva: el alta es responsabilidad
 *   del ABM de administracion, no de este importador).
 * Ambos casos se cuentan y se listan como advertencias en la respuesta, nunca se ignoran en silencio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistorialExcelSyncService {

    private static final int MAX_ADVERTENCIAS = 50;

    private final HistorialExcelReader reader;
    private final AlumnoRepository alumnoRepository;
    private final MateriaRepository materiaRepository;
    private final EstadoAcademicoMateriaRepository estadoRepository;
    private final AuditoriaPublisher auditoriaPublisher;

    @Value("${app.historial.excel-path}")
    private String excelPath;

    /** El arranque del sistema nunca debe depender de que el Excel exista o tenga el formato
     * esperado: cualquier problema queda logueado, no tumba el contexto de Spring. */
    @EventListener(ApplicationReadyEvent.class)
    public void sincronizarAlIniciar() {
        try {
            HistorialImportResultResponse resultado = ejecutar();
            log.info("Historial academico sincronizado desde Excel al iniciar: {} hoja(s), {} fila(s), "
                            + "{} actualizado(s), {} alumno(s) no encontrado(s), {} materia(s) no encontrada(s)",
                    resultado.hojasProcesadas(), resultado.filasLeidas(), resultado.actualizados(),
                    resultado.alumnosNoEncontrados(), resultado.materiasNoEncontradas());
        } catch (Exception e) {
            log.error("No se pudo sincronizar el historial academico desde Excel al iniciar", e);
        }
    }

    /** Version expuesta por API (ADMIN): re-lee el archivo desde disco ahora mismo, por si alguien
     * lo edito externamente, y audita la operacion. */
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public HistorialImportResultResponse sincronizar() {
        HistorialImportResultResponse resultado = ejecutar();
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "HISTORIAL_EXCEL_SINCRONIZADO",
                "EstadoAcademicoMateria", null,
                Map.of(
                        "hojasProcesadas", resultado.hojasProcesadas(),
                        "filasLeidas", resultado.filasLeidas(),
                        "actualizados", resultado.actualizados(),
                        "alumnosNoEncontrados", resultado.alumnosNoEncontrados(),
                        "materiasNoEncontradas", resultado.materiasNoEncontradas()));
        return resultado;
    }

    private HistorialImportResultResponse ejecutar() {
        Path archivo = Path.of(excelPath);
        List<String> advertencias = new ArrayList<>();

        if (!Files.exists(archivo)) {
            advertencias.add("No existe el archivo configurado: " + archivo.toAbsolutePath());
            return new HistorialImportResultResponse(0, 0, 0, 0, 0, LocalDateTime.now(), advertencias);
        }

        List<HistorialExcelRow> filas;
        try {
            filas = reader.leer(archivo);
        } catch (Exception e) {
            log.error("No se pudo leer el historial academico desde {}", archivo, e);
            advertencias.add("El archivo no se pudo leer (formato invalido o corrupto): " + e.getMessage());
            return new HistorialImportResultResponse(0, 0, 0, 0, 0, LocalDateTime.now(), advertencias);
        }

        Set<String> hojas = new HashSet<>();
        int actualizados = 0;
        int alumnosNoEncontrados = 0;
        int materiasNoEncontradas = 0;

        for (HistorialExcelRow fila : filas) {
            hojas.add(fila.hoja());

            List<Alumno> candidatos = alumnoRepository.buscar(null, fila.dni(), null, null);
            if (candidatos.size() != 1) {
                alumnosNoEncontrados++;
                agregarAdvertencia(advertencias, "DNI " + fila.dni() + " (" + fila.hoja() + "): "
                        + (candidatos.isEmpty() ? "no existe como alumno en el sistema" : "coincide con mas de un alumno"));
                continue;
            }
            Alumno alumno = candidatos.get(0);

            for (HistorialExcelMateriaCelda celda : fila.materias()) {
                EstadoMateria estado = resolverEstado(celda.estadoTexto());

                List<Materia> materias = materiaRepository.findByNombreIgnoreCase(celda.nombreMateria());
                if (materias.size() != 1) {
                    materiasNoEncontradas++;
                    agregarAdvertencia(advertencias, "Materia \"" + celda.nombreMateria() + "\" (" + fila.hoja() + "): "
                            + (materias.isEmpty() ? "no encontrada" : "nombre ambiguo, coincide con varias materias"));
                    continue;
                }
                Materia materia = materias.get(0);

                EstadoAcademicoMateria existente = estadoRepository
                        .findByAlumnoIdAndMateriaId(alumno.getUsuarioId(), materia.getId())
                        .orElseGet(() -> EstadoAcademicoMateria.builder().alumno(alumno).materia(materia).build());

                if (estado != null) {
                    existente.setEstado(estado);
                } else if (existente.getEstado() == null) {
                    existente.setEstado(EstadoMateria.PENDIENTE);
                }
                existente.setNota(celda.nota());
                existente.setFechaActualizacion(LocalDateTime.now());
                estadoRepository.save(existente);
                actualizados++;
            }
        }

        return new HistorialImportResultResponse(hojas.size(), filas.size(), actualizados,
                alumnosNoEncontrados, materiasNoEncontradas, LocalDateTime.now(), advertencias);
    }

    private void agregarAdvertencia(List<String> advertencias, String mensaje) {
        if (advertencias.size() < MAX_ADVERTENCIAS) {
            advertencias.add(mensaje);
        }
    }

    private EstadoMateria resolverEstado(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return EstadoMateria.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null; // valor no reconocido (ej. un texto libre que no es un EstadoMateria valido)
        }
    }
}
