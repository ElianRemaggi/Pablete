package com.instituto.siga.repository;

import com.instituto.siga.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByDni(String dni);

    boolean existsByDni(String dni);

    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT u FROM Usuario u LEFT JOIN u.roles r "
            + "WHERE (:rol IS NULL OR r.nombre = :rol) AND (:activo IS NULL OR u.activo = :activo)")
    Page<Usuario> buscar(@Param("rol") String rol, @Param("activo") Boolean activo, Pageable pageable);
}
