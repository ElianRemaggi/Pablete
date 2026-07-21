package com.instituto.siga.controller;

import com.instituto.siga.dto.request.ConfiguracionInstitucionalRequest;
import com.instituto.siga.dto.response.ConfiguracionInstitucionalResponse;
import com.instituto.siga.service.interfaces.ConfiguracionInstitucionalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/configuracion")
@RequiredArgsConstructor
public class ConfiguracionInstitucionalController {

    private final ConfiguracionInstitucionalService configuracionService;

    @GetMapping
    public ResponseEntity<ConfiguracionInstitucionalResponse> obtener() {
        return ResponseEntity.ok(configuracionService.obtener());
    }

    @PutMapping
    public ResponseEntity<ConfiguracionInstitucionalResponse> actualizar(
            @Valid @RequestBody ConfiguracionInstitucionalRequest request) {
        return ResponseEntity.ok(configuracionService.actualizar(request));
    }
}
