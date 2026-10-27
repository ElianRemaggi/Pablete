package com.instituto.siga.controller;

import com.instituto.siga.dto.response.HistorialImportResultResponse;
import com.instituto.siga.dto.response.ImportacionHistorialResponse;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.excel.HistorialExcelImportService;
import com.instituto.siga.excel.HistorialExcelSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/historial-academico")
@RequiredArgsConstructor
public class HistorialAcademicoController {

    private final HistorialExcelSyncService historialExcelSyncService;
    private final HistorialExcelImportService historialExcelImportService;

    /** Relee el .xlsx configurado desde disco y vuelca sus cambios a la base (solo ADMIN,
     * verificado en el service). Util cuando alguien edito el archivo por fuera del sistema. */
    @PostMapping("/recargar")
    public ResponseEntity<HistorialImportResultResponse> recargar() {
        return ResponseEntity.ok(historialExcelSyncService.sincronizar());
    }

    /** Sube un historial .xlsx. Con {@code confirmar=false} (default) solo valida y devuelve la vista previa sin
     * modificar nada; con {@code confirmar=true} aplica exactamente lo mismo (PRECEPTOR o ADMIN, verificado en el service). */
    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacionHistorialResponse> importar(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(defaultValue = "false") boolean confirmar,
            @RequestParam(defaultValue = "false") boolean crearAlumnos) throws IOException {
        String nombre = archivo.getOriginalFilename() == null ? "historial.xlsx" : archivo.getOriginalFilename();
        if (!nombre.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BusinessException("ARCHIVO_INVALIDO", "El archivo debe ser un libro de Excel con extensión .xlsx");
        }
        return ResponseEntity.ok(historialExcelImportService.procesar(nombre, archivo.getBytes(), confirmar, crearAlumnos));
    }

    /** Plantilla vacia con las hojas y encabezados que espera el importador. */
    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> plantilla() throws IOException {
        byte[] contenido = new ClassPathResource("historial-academico-plantilla.xlsx").getInputStream().readAllBytes();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"historial-academico-plantilla.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(contenido);
    }
}
