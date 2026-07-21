package com.instituto.siga.controller;

import com.instituto.siga.dto.request.CarreraRequest;
import com.instituto.siga.dto.response.CarreraResponse;
import com.instituto.siga.service.interfaces.CarreraService;
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
@RequestMapping("/api/v1/carreras")
@RequiredArgsConstructor
public class CarreraController {

    private final CarreraService carreraService;

    @GetMapping
    public ResponseEntity<List<CarreraResponse>> listar() {
        return ResponseEntity.ok(carreraService.listar());
    }

    @PostMapping
    public ResponseEntity<CarreraResponse> crear(@Valid @RequestBody CarreraRequest request) {
        return ResponseEntity.ok(carreraService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarreraResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody CarreraRequest request) {
        return ResponseEntity.ok(carreraService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id) {
        carreraService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }
}
