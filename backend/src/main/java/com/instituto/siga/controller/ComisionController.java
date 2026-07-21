package com.instituto.siga.controller;

import com.instituto.siga.dto.request.ComisionRequest;
import com.instituto.siga.dto.request.HorarioRequest;
import com.instituto.siga.dto.response.ComisionResponse;
import com.instituto.siga.service.interfaces.ComisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comisiones")
@RequiredArgsConstructor
public class ComisionController {

    private final ComisionService comisionService;

    @GetMapping
    public ResponseEntity<List<ComisionResponse>> listar(
            @RequestParam(required = false) Long materiaId,
            @RequestParam(required = false) String periodoLectivo) {
        return ResponseEntity.ok(comisionService.listar(materiaId, periodoLectivo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComisionResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(comisionService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<ComisionResponse> crear(@Valid @RequestBody ComisionRequest request) {
        return ResponseEntity.ok(comisionService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ComisionResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody ComisionRequest request) {
        return ResponseEntity.ok(comisionService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        comisionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/horarios")
    public ResponseEntity<ComisionResponse> agregarHorario(@PathVariable Long id,
                                                            @Valid @RequestBody HorarioRequest request) {
        return ResponseEntity.ok(comisionService.agregarHorario(id, request));
    }

    @DeleteMapping("/{id}/horarios/{horarioId}")
    public ResponseEntity<ComisionResponse> quitarHorario(@PathVariable Long id, @PathVariable Long horarioId) {
        return ResponseEntity.ok(comisionService.quitarHorario(id, horarioId));
    }
}
