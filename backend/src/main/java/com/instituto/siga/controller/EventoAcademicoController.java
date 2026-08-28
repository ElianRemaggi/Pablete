package com.instituto.siga.controller;

import com.instituto.siga.dto.request.EventoAcademicoRequest;
import com.instituto.siga.dto.response.EventoAcademicoResponse;
import com.instituto.siga.service.interfaces.EventoAcademicoService;
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
@RequestMapping("/api/v1/eventos-academicos")
@RequiredArgsConstructor
public class EventoAcademicoController {

    private final EventoAcademicoService eventoAcademicoService;

    @GetMapping
    public ResponseEntity<List<EventoAcademicoResponse>> listar() {
        return ResponseEntity.ok(eventoAcademicoService.listar());
    }

    @PostMapping
    public ResponseEntity<EventoAcademicoResponse> crear(@Valid @RequestBody EventoAcademicoRequest request) {
        return ResponseEntity.ok(eventoAcademicoService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventoAcademicoResponse> actualizar(@PathVariable Long id,
                                                                @Valid @RequestBody EventoAcademicoRequest request) {
        return ResponseEntity.ok(eventoAcademicoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        eventoAcademicoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
