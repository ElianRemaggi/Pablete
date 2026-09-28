package com.instituto.siga.controller;

import com.instituto.siga.dto.response.HistorialImportResultResponse;
import com.instituto.siga.excel.HistorialExcelSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/historial-academico")
@RequiredArgsConstructor
public class HistorialAcademicoController {

    private final HistorialExcelSyncService historialExcelSyncService;

    /** Relee el .xlsx configurado desde disco y vuelca sus cambios a la base (solo ADMIN,
     * verificado en el service). Util cuando alguien edito el archivo por fuera del sistema. */
    @PostMapping("/recargar")
    public ResponseEntity<HistorialImportResultResponse> recargar() {
        return ResponseEntity.ok(historialExcelSyncService.sincronizar());
    }
}
