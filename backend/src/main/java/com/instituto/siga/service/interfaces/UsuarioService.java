package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.request.UsuarioRequest;
import com.instituto.siga.dto.request.UsuarioRolesRequest;
import com.instituto.siga.dto.request.UsuarioUpdateRequest;
import com.instituto.siga.dto.response.UsuarioResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {

    Page<UsuarioResponse> listar(String rol, Boolean activo, Pageable pageable);

    UsuarioResponse obtenerPorId(Long id);

    UsuarioResponse obtenerActual(Long usuarioId);

    UsuarioResponse crear(UsuarioRequest request);

    UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request);

    UsuarioResponse activar(Long id);

    UsuarioResponse desactivar(Long id);

    UsuarioResponse actualizarRoles(Long id, UsuarioRolesRequest request);
}
