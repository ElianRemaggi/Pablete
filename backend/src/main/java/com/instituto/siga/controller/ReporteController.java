package com.instituto.siga.controller;

import com.instituto.siga.dto.response.TasaAprobacionResponse;
import com.instituto.siga.service.interfaces.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/tasa-aprobacion-por-materia")
    public ResponseEntity<List<TasaAprobacionResponse>> tasaAprobacionPorMateria() {
        return ResponseEntity.ok(reporteService.tasaAprobacionPorMateria());
    }
}
