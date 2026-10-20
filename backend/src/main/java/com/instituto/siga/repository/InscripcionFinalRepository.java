package com.instituto.siga.repository;

import com.instituto.siga.entity.EstadoInscripcion;
import com.instituto.siga.entity.InscripcionFinal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InscripcionFinalRepository extends JpaRepository<InscripcionFinal, Long> {

    /**
     * Los metodos "...AlumnoId..." usan @Query explicito: el @Id de Alumno se llama "usuarioId"
     * (por @MapsId a Usuario), no "id", y Spring Data no deriva "AlumnoId" contra eso
     * (ver InscripcionCursadaRepository, mismo problema).
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM InscripcionFinal i "
            + "WHERE i.alumno.usuarioId = :alumnoId AND i.mesa.id = :mesaId AND i.estado IN :estados")
    boolean existsByAlumnoIdAndMesaIdAndEstadoIn(@Param("alumnoId") Long alumnoId, @Param("mesaId") Long mesaId,
                                                  @Param("estados") List<EstadoInscripcion> estados);

    @Query("SELECT i FROM InscripcionFinal i WHERE i.alumno.usuarioId = :alumnoId AND i.estado IN :estados")
    List<InscripcionFinal> findByAlumnoIdAndEstadoIn(@Param("alumnoId") Long alumnoId, @Param("estados") List<EstadoInscripcion> estados);

    List<InscripcionFinal> findByMesaId(Long mesaId);

    long countByMesaIdAndEstadoIn(Long mesaId, List<EstadoInscripcion> estados);

    boolean existsByMesaId(Long mesaId);

    @Query("SELECT i FROM InscripcionFinal i WHERE i.id = :id AND i.alumno.usuarioId = :alumnoId")
    Optional<InscripcionFinal> findByIdAndAlumnoId(@Param("id") Long id, @Param("alumnoId") Long alumnoId);

    @Query("SELECT i FROM InscripcionFinal i WHERE i.alumno.usuarioId = :alumnoId")
    List<InscripcionFinal> findByAlumnoId(@Param("alumnoId") Long alumnoId);
}
