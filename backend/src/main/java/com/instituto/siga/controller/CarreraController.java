package com.instituto.siga.controller;

import com.instituto.siga.dto.request.CarreraRequest;
import com.instituto.siga.dto.request.PlanEstudioRequest;
import com.instituto.siga.dto.response.CarreraResponse;
import com.instituto.siga.dto.response.PlanEstudioResponse;
import com.instituto.siga.service.interfaces.CarreraService;
import com.instituto.siga.service.interfaces.PlanEstudioService;
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
    private final PlanEstudioService planEstudioService;

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

    @GetMapping("/{id}/planes")
    public ResponseEntity<List<PlanEstudioResponse>> listarPlanes(@PathVariable Long id) {
        return ResponseEntity.ok(planEstudioService.listarPorCarrera(id));
    }

    @PostMapping("/{id}/planes")
    public ResponseEntity<PlanEstudioResponse> crearPlan(@PathVariable Long id,
                                                          @Valid @RequestBody PlanEstudioRequest request) {
        return ResponseEntity.ok(planEstudioService.crear(id, request));
    }
}
