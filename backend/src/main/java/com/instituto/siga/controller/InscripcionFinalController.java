package com.instituto.siga.controller;

import com.instituto.siga.dto.request.InscripcionFinalRequest;
import com.instituto.siga.dto.request.ResultadoFinalRequest;
import com.instituto.siga.dto.response.InscripcionFinalResponse;
import com.instituto.siga.service.interfaces.InscripcionFinalService;
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
@RequestMapping("/api/v1/inscripciones-final")
@RequiredArgsConstructor
public class InscripcionFinalController {

    private final InscripcionFinalService inscripcionFinalService;

    @GetMapping("/me")
    public ResponseEntity<List<InscripcionFinalResponse>> listarPropias(@AuthenticationPrincipal Long alumnoId) {
        return ResponseEntity.ok(inscripcionFinalService.listarPropias(alumnoId));
    }

    @PostMapping
    public ResponseEntity<InscripcionFinalResponse> inscribir(@AuthenticationPrincipal Long alumnoId,
                                                               @Valid @RequestBody InscripcionFinalRequest request) {
        return ResponseEntity.ok(inscripcionFinalService.inscribir(alumnoId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InscripcionFinalResponse> modificar(@AuthenticationPrincipal Long alumnoId,
                                                               @PathVariable Long id,
                                                               @Valid @RequestBody InscripcionFinalRequest request) {
        return ResponseEntity.ok(inscripcionFinalService.modificar(alumnoId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@AuthenticationPrincipal Long alumnoId, @PathVariable Long id) {
        inscripcionFinalService.darDeBaja(alumnoId, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/resultado")
    public ResponseEntity<InscripcionFinalResponse> registrarResultado(@PathVariable Long id,
                                                                        @Valid @RequestBody ResultadoFinalRequest request) {
        return ResponseEntity.ok(inscripcionFinalService.registrarResultado(id, request));
    }

    @GetMapping("/mesa/{mesaId}")
    public ResponseEntity<List<InscripcionFinalResponse>> listarPorMesa(@PathVariable Long mesaId) {
        return ResponseEntity.ok(inscripcionFinalService.listarPorMesa(mesaId));
    }

    @GetMapping("/alumno/{alumnoId}")
    public ResponseEntity<List<InscripcionFinalResponse>> listarPorAlumno(@PathVariable Long alumnoId) {
        return ResponseEntity.ok(inscripcionFinalService.listarPorAlumno(alumnoId));
    }
}
