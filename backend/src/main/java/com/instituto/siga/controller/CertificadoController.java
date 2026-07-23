package com.instituto.siga.controller;

import com.instituto.siga.dto.response.CertificadoResponse;
import com.instituto.siga.service.interfaces.CertificadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/certificados")
@RequiredArgsConstructor
public class CertificadoController {

    private final CertificadoService certificadoService;

    @GetMapping("/alumno-regular")
    public ResponseEntity<byte[]> alumnoRegularPropio(@AuthenticationPrincipal Long usuarioId) {
        return pdf(certificadoService.generarAlumnoRegularPropio(usuarioId), "alumno-regular.pdf");
    }

    @GetMapping("/analitico-parcial")
    public ResponseEntity<byte[]> analiticoParcialPropio(@AuthenticationPrincipal Long usuarioId) {
        return pdf(certificadoService.generarAnaliticoParcialPropio(usuarioId), "analitico-parcial.pdf");
    }

    @GetMapping("/alumno/{id}/alumno-regular")
    public ResponseEntity<byte[]> alumnoRegularParaAlumno(@PathVariable Long id) {
        return pdf(certificadoService.generarAlumnoRegularParaAlumno(id), "alumno-regular.pdf");
    }

    @GetMapping("/historial")
    public ResponseEntity<List<CertificadoResponse>> historial(@AuthenticationPrincipal Long usuarioId) {
        return ResponseEntity.ok(certificadoService.historialPropio(usuarioId));
    }

    private ResponseEntity<byte[]> pdf(byte[] contenido, String nombreArchivo) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .body(contenido);
    }
}
