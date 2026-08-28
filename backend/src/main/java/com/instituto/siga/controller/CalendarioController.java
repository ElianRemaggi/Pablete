package com.instituto.siga.controller;

import com.instituto.siga.dto.response.EventoCalendarioResponse;
import com.instituto.siga.service.interfaces.CalendarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calendario")
@RequiredArgsConstructor
public class CalendarioController {

    private final CalendarioService calendarioService;

    @GetMapping("/me")
    public ResponseEntity<List<EventoCalendarioResponse>> propio(
            @AuthenticationPrincipal Long alumnoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(calendarioService.calendarioPropio(alumnoId, desde, hasta));
    }

    @GetMapping("/alumno/{alumnoId}")
    public ResponseEntity<List<EventoCalendarioResponse>> deAlumno(
            @PathVariable Long alumnoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(calendarioService.calendarioDeAlumno(alumnoId, desde, hasta));
    }

    @GetMapping("/institucional")
    public ResponseEntity<List<EventoCalendarioResponse>> institucional(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(calendarioService.calendarioInstitucional(desde, hasta));
    }
}
