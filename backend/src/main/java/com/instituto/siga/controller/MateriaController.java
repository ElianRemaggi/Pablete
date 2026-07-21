package com.instituto.siga.controller;

import com.instituto.siga.dto.request.MateriaRequest;
import com.instituto.siga.dto.response.MateriaResponse;
import com.instituto.siga.service.interfaces.MateriaService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/materias")
@RequiredArgsConstructor
public class MateriaController {

    private final MateriaService materiaService;

    @GetMapping
    public ResponseEntity<List<MateriaResponse>> listar() {
        return ResponseEntity.ok(materiaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MateriaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(materiaService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<MateriaResponse> crear(@Valid @RequestBody MateriaRequest request) {
        return ResponseEntity.ok(materiaService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MateriaResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody MateriaRequest request) {
        return ResponseEntity.ok(materiaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id) {
        materiaService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }
}
