package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.request.ClaveNuevaRequest;
import com.instituto.siga.dto.request.PerfilUpdateRequest;
import com.instituto.siga.dto.request.RegistroRequest;
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

    /** Autoregistro publico: crea un Usuario con rol ALUMNO unicamente (y su fila de Alumno). */
    UsuarioResponse registrarAlumno(RegistroRequest request);

    UsuarioResponse actualizarPropio(Long usuarioId, PerfilUpdateRequest request);

    UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request);

    UsuarioResponse activar(Long id);

    UsuarioResponse desactivar(Long id);

    UsuarioResponse actualizarRoles(Long id, UsuarioRolesRequest request);

    /** El administrador crea una clave nueva para el usuario (recuperacion de clave olvidada); cierra sus sesiones. */
    void crearNuevaClave(Long id, ClaveNuevaRequest request);
}
