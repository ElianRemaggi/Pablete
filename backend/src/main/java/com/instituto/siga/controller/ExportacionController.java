package com.instituto.siga.controller;

import com.instituto.siga.service.interfaces.ExportacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exportaciones")
@RequiredArgsConstructor
public class ExportacionController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ExportacionService exportacionService;

    @GetMapping("/inscripciones-cursada")
    public ResponseEntity<byte[]> inscripcionesCursada(@RequestParam Long comisionId) {
        return excel(exportacionService.exportarInscripcionesCursada(comisionId), "inscripciones-cursada.xlsx");
    }

    @GetMapping("/inscripciones-final")
    public ResponseEntity<byte[]> inscripcionesFinal(@RequestParam Long mesaId) {
        return excel(exportacionService.exportarInscripcionesFinal(mesaId), "inscripciones-final.xlsx");
    }

    @GetMapping("/alumnos")
    public ResponseEntity<byte[]> alumnos(@RequestParam(required = false) String legajo,
                                          @RequestParam(required = false) String dni,
                                          @RequestParam(required = false) String apellido) {
        return excel(exportacionService.exportarAlumnos(legajo, dni, apellido), "alumnos.xlsx");
    }

    private ResponseEntity<byte[]> excel(byte[] contenido, String nombreArchivo) {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .body(contenido);
    }
}
