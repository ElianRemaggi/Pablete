package com.instituto.siga.repository;

import com.instituto.siga.entity.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * Solo se usan save/findById/la busqueda paginada — nunca delete (docs/08 SS6, append-only).
 * JpaRepository expone delete por herencia estandar de Spring Data, pero ningun Service de
 * esta app lo invoca; el borrado real se restringe a nivel de permisos de BD en produccion.
 */
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    @Query("SELECT a FROM Auditoria a WHERE "
            + "(:usuarioId IS NULL OR a.usuario.id = :usuarioId) AND "
            + "(:accion IS NULL OR a.accion = :accion) AND "
            + "(:entidad IS NULL OR a.entidad = :entidad) AND "
            + "(:fechaDesde IS NULL OR a.fecha >= :fechaDesde) AND "
            + "(:fechaHasta IS NULL OR a.fecha <= :fechaHasta) "
            + "ORDER BY a.fecha DESC")
    Page<Auditoria> buscar(@Param("usuarioId") Long usuarioId,
                           @Param("accion") String accion,
                           @Param("entidad") String entidad,
                           @Param("fechaDesde") LocalDateTime fechaDesde,
                           @Param("fechaHasta") LocalDateTime fechaHasta,
                           Pageable pageable);
}
