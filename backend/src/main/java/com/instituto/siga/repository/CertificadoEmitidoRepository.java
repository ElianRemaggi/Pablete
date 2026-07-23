package com.instituto.siga.repository;

import com.instituto.siga.entity.CertificadoEmitido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CertificadoEmitidoRepository extends JpaRepository<CertificadoEmitido, Long> {

    /**
     * @Query explicito, no derivado: el @Id de Alumno se llama "usuarioId" (por @MapsId a
     * Usuario), no "id" — ver el mismo caso ya resuelto en EstadoAcademicoMateriaRepository.
     */
    @Query("SELECT c FROM CertificadoEmitido c WHERE c.alumno.usuarioId = :alumnoId ORDER BY c.fechaEmision DESC")
    List<CertificadoEmitido> findByAlumnoId(@Param("alumnoId") Long alumnoId);
}
