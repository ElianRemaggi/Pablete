package com.instituto.siga.repository;

import com.instituto.siga.entity.EstadoInscripcion;
import com.instituto.siga.entity.InscripcionCursada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InscripcionCursadaRepository extends JpaRepository<InscripcionCursada, Long> {

    /**
     * Los metodos "...AlumnoId..." usan @Query explicito, no derivacion automatica: el @Id de
     * Alumno se llama "usuarioId" (por @MapsId a Usuario), no "id", y Spring Data no puede
     * resolver "AlumnoId" contra una propiedad que no se llama literalmente "id".
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM InscripcionCursada i "
            + "WHERE i.alumno.usuarioId = :alumnoId AND i.comision.id = :comisionId AND i.estado IN :estados")
    boolean existsByAlumnoIdAndComisionIdAndEstadoIn(@Param("alumnoId") Long alumnoId, @Param("comisionId") Long comisionId,
                                                      @Param("estados") List<EstadoInscripcion> estados);

    long countByComisionIdAndEstadoIn(Long comisionId, List<EstadoInscripcion> estados);

    @Query("SELECT i FROM InscripcionCursada i WHERE i.alumno.usuarioId = :alumnoId AND i.estado IN :estados")
    List<InscripcionCursada> findByAlumnoIdAndEstadoIn(@Param("alumnoId") Long alumnoId, @Param("estados") List<EstadoInscripcion> estados);

    List<InscripcionCursada> findByComisionId(Long comisionId);

    @Query("SELECT i FROM InscripcionCursada i WHERE i.id = :id AND i.alumno.usuarioId = :alumnoId")
    Optional<InscripcionCursada> findByIdAndAlumnoId(@Param("id") Long id, @Param("alumnoId") Long alumnoId);
}
