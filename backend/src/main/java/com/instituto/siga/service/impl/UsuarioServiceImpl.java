package com.instituto.siga.service.impl;

import com.instituto.siga.audit.AuditoriaPublisher;
import com.instituto.siga.dto.request.ClaveNuevaRequest;
import com.instituto.siga.dto.request.PerfilUpdateRequest;
import com.instituto.siga.repository.RefreshTokenRepository;
import com.instituto.siga.dto.request.RegistroRequest;
import com.instituto.siga.dto.request.UsuarioRequest;
import com.instituto.siga.dto.request.UsuarioRolesRequest;
import com.instituto.siga.dto.request.UsuarioUpdateRequest;
import com.instituto.siga.dto.response.UsuarioResponse;
import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.Rol;
import com.instituto.siga.entity.Usuario;
import com.instituto.siga.exception.BusinessException;
import com.instituto.siga.exception.ConflictException;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.mapper.UsuarioMapper;
import com.instituto.siga.repository.AlumnoRepository;
import com.instituto.siga.repository.RolRepository;
import com.instituto.siga.repository.UsuarioRepository;
import com.instituto.siga.security.SecurityUtils;
import com.instituto.siga.service.interfaces.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private static final String ROL_ALUMNO = "ALUMNO";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AlumnoRepository alumnoRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaPublisher auditoriaPublisher;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(String rol, Boolean activo, Pageable pageable) {
        return usuarioRepository.buscar(rol, activo, pageable).map(usuarioMapper::toResponse);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        return usuarioMapper.toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse obtenerActual(Long usuarioId) {
        return usuarioMapper.toResponse(buscarOFallar(usuarioId));
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        return crearUsuario(request, "USUARIO_CREADO");
    }

    @Override
    @Transactional
    public UsuarioResponse registrarAlumno(RegistroRequest request) {
        UsuarioRequest alta = new UsuarioRequest(request.dni().trim(), request.email().trim(), request.nombre().trim(),
                request.apellido().trim(), request.password(), Set.of(ROL_ALUMNO));
        return crearUsuario(alta, "USUARIO_REGISTRADO");
    }

    @Override
    @Transactional
    public UsuarioResponse actualizarPropio(Long usuarioId, PerfilUpdateRequest request) {
        Usuario usuario = buscarOFallar(usuarioId);
        String email = request.email().trim();
        if (!email.equalsIgnoreCase(usuario.getEmail()) && usuarioRepository.existsByEmail(email)) {
            throw new ConflictException("EMAIL_DUPLICADO", "Ya existe un usuario con ese email");
        }
        usuario.setEmail(email);
        Usuario guardado = usuarioRepository.save(usuario);
        auditoriaPublisher.publicar(usuarioId, "USUARIO_PERFIL_MODIFICADO", "Usuario", usuarioId, Map.of("email", email));
        return usuarioMapper.toResponse(guardado);
    }

    private UsuarioResponse crearUsuario(UsuarioRequest request, String accionAuditoria) {
        if (usuarioRepository.existsByDni(request.dni())) {
            throw new ConflictException("DNI_DUPLICADO", "Ya existe un usuario con ese DNI");
        }
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflictException("EMAIL_DUPLICADO", "Ya existe un usuario con ese email");
        }

        Usuario usuario = usuarioMapper.toEntity(request);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setActivo(true);
        usuario.setRoles(resolverRoles(request.roles()));

        Usuario guardado = usuarioRepository.save(usuario);
        asegurarAlumnoSiCorresponde(guardado);
        // usuarioActualId() es null en el autoregistro (no hay sesion): el evento queda sin actor, con el DNI en el detalle.
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), accionAuditoria, "Usuario", guardado.getId(),
                Map.of("dni", guardado.getDni(), "roles", request.roles()));

        return usuarioMapper.toResponse(guardado);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = buscarOFallar(id);

        if (request.email() != null && !request.email().equals(usuario.getEmail())) {
            if (usuarioRepository.existsByEmail(request.email())) {
                throw new ConflictException("EMAIL_DUPLICADO", "Ya existe un usuario con ese email");
            }
            usuario.setEmail(request.email());
        }
        if (request.nombre() != null) {
            usuario.setNombre(request.nombre());
        }
        if (request.apellido() != null) {
            usuario.setApellido(request.apellido());
        }
        if (request.activo() != null) {
            usuario.setActivo(request.activo());
        }

        Usuario guardado = usuarioRepository.save(usuario);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "USUARIO_MODIFICADO", "Usuario", guardado.getId(),
                Map.of("email", guardado.getEmail(), "nombre", guardado.getNombre(),
                        "apellido", guardado.getApellido(), "activo", guardado.isActivo()));

        return usuarioMapper.toResponse(guardado);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public UsuarioResponse activar(Long id) {
        Usuario usuario = buscarOFallar(id);
        usuario.setActivo(true);
        Usuario guardado = usuarioRepository.save(usuario);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "USUARIO_ACTIVADO", "Usuario", guardado.getId(), Map.of());
        return usuarioMapper.toResponse(guardado);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public UsuarioResponse desactivar(Long id) {
        Usuario usuario = buscarOFallar(id);
        usuario.setActivo(false);
        Usuario guardado = usuarioRepository.save(usuario);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "USUARIO_DESACTIVADO", "Usuario", guardado.getId(), Map.of());
        return usuarioMapper.toResponse(guardado);
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public void crearNuevaClave(Long id, ClaveNuevaRequest request) {
        Usuario usuario = buscarOFallar(id);
        usuario.setPasswordHash(passwordEncoder.encode(request.nuevaClave()));
        usuarioRepository.save(usuario);
        refreshTokenRepository.revocarTodosDelUsuario(id);
        // Nunca se audita la clave, solo el hecho.
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "USUARIO_CLAVE_RESTABLECIDA", "Usuario", id, Map.of());
    }

    @Override
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public UsuarioResponse actualizarRoles(Long id, UsuarioRolesRequest request) {
        Usuario usuario = buscarOFallar(id);
        usuario.setRoles(resolverRoles(request.roles()));
        Usuario guardado = usuarioRepository.save(usuario);
        asegurarAlumnoSiCorresponde(guardado);
        auditoriaPublisher.publicar(SecurityUtils.usuarioActualId(), "USUARIO_ROLES_ACTUALIZADOS", "Usuario",
                guardado.getId(), Map.of("roles", request.roles()));
        return usuarioMapper.toResponse(guardado);
    }

    private Usuario buscarOFallar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
    }

    /**
     * docs/03 modela Alumno como subtipo 1:1 de Usuario, pero no hay endpoint documentado de
     * alta separada — se crea aca, al asignar el rol, con legajo generado deterministicamente.
     */
    private void asegurarAlumnoSiCorresponde(Usuario usuario) {
        boolean esAlumno = usuario.getRoles().stream().anyMatch(r -> ROL_ALUMNO.equals(r.getNombre()));
        if (esAlumno && !alumnoRepository.existsById(usuario.getId())) {
            Alumno alumno = Alumno.builder()
                    .usuario(usuario)
                    .legajo("LEG-" + String.format("%04d", usuario.getId()))
                    .fechaIngreso(LocalDate.now())
                    .build();
            alumnoRepository.save(alumno);
        }
    }

    private Set<Rol> resolverRoles(Set<String> nombresRoles) {
        return nombresRoles.stream()
                .map(nombre -> rolRepository.findByNombre(nombre)
                        .orElseThrow(() -> new BusinessException("ROL_INVALIDO", "Rol inexistente: " + nombre)))
                .collect(Collectors.toSet());
    }
}
