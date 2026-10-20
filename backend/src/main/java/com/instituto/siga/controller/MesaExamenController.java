package com.instituto.siga.controller;

import com.instituto.siga.dto.request.MesaExamenRequest;
import com.instituto.siga.dto.response.MesaExamenResponse;
import com.instituto.siga.entity.EstadoMesa;
import com.instituto.siga.service.interfaces.MesaExamenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/mesas-examen")
@RequiredArgsConstructor
public class MesaExamenController {

    private final MesaExamenService mesaExamenService;

    @GetMapping
    public ResponseEntity<List<MesaExamenResponse>> listar(
            @RequestParam(required = false) Long materiaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) EstadoMesa estado,
            @RequestParam(required = false) String periodo) {
        return ResponseEntity.ok(mesaExamenService.listar(materiaId, fecha, estado, periodo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MesaExamenResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(mesaExamenService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<MesaExamenResponse> crear(@Valid @RequestBody MesaExamenRequest request) {
        return ResponseEntity.ok(mesaExamenService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MesaExamenResponse> actualizar(@PathVariable Long id,
                                                          @Valid @RequestBody MesaExamenRequest request) {
        return ResponseEntity.ok(mesaExamenService.actualizar(id, request));
    }

    @PatchMapping("/{id}/publicar")
    public ResponseEntity<MesaExamenResponse> publicar(@PathVariable Long id) {
        return ResponseEntity.ok(mesaExamenService.publicar(id));
    }

    @PatchMapping("/{id}/cerrar")
    public ResponseEntity<MesaExamenResponse> cerrar(@PathVariable Long id) {
        return ResponseEntity.ok(mesaExamenService.cerrar(id));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<MesaExamenResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(mesaExamenService.cancelar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        mesaExamenService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
