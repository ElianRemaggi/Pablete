package com.instituto.siga.controller;

import com.instituto.siga.dto.request.PerfilUpdateRequest;
import com.instituto.siga.dto.request.UsuarioRequest;
import com.instituto.siga.dto.request.UsuarioRolesRequest;
import com.instituto.siga.dto.request.UsuarioUpdateRequest;
import com.instituto.siga.dto.response.UsuarioResponse;
import com.instituto.siga.service.interfaces.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<Page<UsuarioResponse>> listar(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) Boolean activo,
            Pageable pageable) {
        return ResponseEntity.ok(usuarioService.listar(rol, activo, pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> obtenerActual(@AuthenticationPrincipal Long usuarioId) {
        return ResponseEntity.ok(usuarioService.obtenerActual(usuarioId));
    }

    @PutMapping("/me")
    public ResponseEntity<UsuarioResponse> actualizarPropio(@AuthenticationPrincipal Long usuarioId,
                                                             @Valid @RequestBody PerfilUpdateRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarPropio(usuarioId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody UsuarioUpdateRequest request) {
        return ResponseEntity.ok(usuarioService.actualizar(id, request));
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<UsuarioResponse> activar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.activar(id));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<UsuarioResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.desactivar(id));
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<UsuarioResponse> actualizarRoles(@PathVariable Long id,
                                                            @Valid @RequestBody UsuarioRolesRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarRoles(id, request));
    }
}
