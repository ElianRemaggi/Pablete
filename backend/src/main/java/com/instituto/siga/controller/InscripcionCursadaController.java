package com.instituto.siga.controller;

import com.instituto.siga.dto.request.InscripcionCursadaRequest;
import com.instituto.siga.dto.response.InscripcionCursadaResponse;
import com.instituto.siga.service.interfaces.InscripcionCursadaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inscripciones-cursada")
@RequiredArgsConstructor
public class InscripcionCursadaController {

    private final InscripcionCursadaService inscripcionCursadaService;

    @GetMapping("/me")
    public ResponseEntity<List<InscripcionCursadaResponse>> listarPropias(@AuthenticationPrincipal Long alumnoId) {
        return ResponseEntity.ok(inscripcionCursadaService.listarPropias(alumnoId));
    }

    @PostMapping
    public ResponseEntity<InscripcionCursadaResponse> inscribir(@AuthenticationPrincipal Long alumnoId,
                                                                 @Valid @RequestBody InscripcionCursadaRequest request) {
        return ResponseEntity.ok(inscripcionCursadaService.inscribir(alumnoId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InscripcionCursadaResponse> modificar(@AuthenticationPrincipal Long alumnoId,
                                                                 @PathVariable Long id,
                                                                 @Valid @RequestBody InscripcionCursadaRequest request) {
        return ResponseEntity.ok(inscripcionCursadaService.modificar(alumnoId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@AuthenticationPrincipal Long alumnoId, @PathVariable Long id) {
        inscripcionCursadaService.darDeBaja(alumnoId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/comision/{comisionId}")
    public ResponseEntity<List<InscripcionCursadaResponse>> listarPorComision(@PathVariable Long comisionId) {
        return ResponseEntity.ok(inscripcionCursadaService.listarPorComision(comisionId));
    }

    @GetMapping("/alumno/{alumnoId}")
    public ResponseEntity<List<InscripcionCursadaResponse>> listarPorAlumno(@PathVariable Long alumnoId) {
        return ResponseEntity.ok(inscripcionCursadaService.listarPorAlumno(alumnoId));
    }
}
