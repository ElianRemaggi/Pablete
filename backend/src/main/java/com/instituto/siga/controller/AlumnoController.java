package com.instituto.siga.controller;

import com.instituto.siga.dto.request.EstadoAcademicoRequest;
import com.instituto.siga.dto.response.AlumnoResponse;
import com.instituto.siga.dto.response.EstadoAcademicoResponse;
import com.instituto.siga.dto.response.HistorialAcademicoResponse;
import com.instituto.siga.service.interfaces.AlumnoService;
import com.instituto.siga.service.interfaces.EstadoAcademicoService;
import com.instituto.siga.service.interfaces.RequisitosAcademicosService;
import com.instituto.siga.dto.response.HabilitacionFinalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alumnos")
@RequiredArgsConstructor
public class AlumnoController {

    private final EstadoAcademicoService estadoAcademicoService;
    private final AlumnoService alumnoService;
    private final RequisitosAcademicosService requisitosService;

    @GetMapping("/me")
    public ResponseEntity<AlumnoResponse> obtenerPropio(@AuthenticationPrincipal Long usuarioId) {
        return ResponseEntity.ok(alumnoService.obtenerPropio(usuarioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlumnoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(alumnoService.obtenerPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<AlumnoResponse>> buscar(
            @RequestParam(required = false) String legajo,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String apellido,
            @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(alumnoService.buscar(legajo, dni, apellido, nombre));
    }

    @GetMapping("/me/historial-academico")
    public ResponseEntity<List<HistorialAcademicoResponse>> historialPropio(@AuthenticationPrincipal Long usuarioId) {
        return ResponseEntity.ok(alumnoService.historialPropio(usuarioId));
    }

    @GetMapping("/{id}/historial-academico")
    public ResponseEntity<List<HistorialAcademicoResponse>> historialPorId(@PathVariable Long id) {
        return ResponseEntity.ok(alumnoService.historialPorId(id));
    }

    @GetMapping("/me/materias-aprobadas")
    public ResponseEntity<List<HistorialAcademicoResponse>> materiasAprobadasPropias(
            @AuthenticationPrincipal Long usuarioId) {
        return ResponseEntity.ok(alumnoService.materiasAprobadasPropias(usuarioId));
    }

    @GetMapping("/me/materias-pendientes")
    public ResponseEntity<List<HistorialAcademicoResponse>> materiasPendientesPropias(
            @AuthenticationPrincipal Long usuarioId) {
        return ResponseEntity.ok(alumnoService.materiasPendientesPropias(usuarioId));
    }

    /** Que le falta al alumno para inscribirse al final de la materia (correlativas de cursada y de final). */
    @GetMapping("/me/materias/{materiaId}/habilitacion-final")
    public ResponseEntity<HabilitacionFinalResponse> habilitacionFinalPropia(
            @AuthenticationPrincipal Long usuarioId, @PathVariable Long materiaId) {
        return ResponseEntity.ok(requisitosService.habilitacionFinalPropia(usuarioId, materiaId));
    }

    @GetMapping("/{id}/materias/{materiaId}/habilitacion-final")
    public ResponseEntity<HabilitacionFinalResponse> habilitacionFinalDeAlumno(
            @PathVariable Long id, @PathVariable Long materiaId) {
        return ResponseEntity.ok(requisitosService.habilitacionFinalDeAlumno(id, materiaId));
    }

    @PutMapping("/{id}/estado-academico/{materiaId}")
    public ResponseEntity<EstadoAcademicoResponse> fijarEstadoAcademico(
            @PathVariable Long id, @PathVariable Long materiaId, @Valid @RequestBody EstadoAcademicoRequest request) {
        return ResponseEntity.ok(estadoAcademicoService.fijarEstado(id, materiaId, request.estado(), request.nota()));
    }
}
