package com.instituto.siga.controller;

import com.instituto.siga.dto.request.CorrelatividadRequest;
import com.instituto.siga.dto.response.CorrelatividadResponse;
import com.instituto.siga.service.interfaces.CorrelatividadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/correlatividades")
@RequiredArgsConstructor
public class CorrelatividadController {

    private final CorrelatividadService correlatividadService;

    @GetMapping("/materia/{materiaId}")
    public ResponseEntity<List<CorrelatividadResponse>> listarPorMateria(@PathVariable Long materiaId) {
        return ResponseEntity.ok(correlatividadService.listarPorMateria(materiaId));
    }

    @PostMapping
    public ResponseEntity<CorrelatividadResponse> crear(@Valid @RequestBody CorrelatividadRequest request) {
        return ResponseEntity.ok(correlatividadService.crear(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        correlatividadService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
