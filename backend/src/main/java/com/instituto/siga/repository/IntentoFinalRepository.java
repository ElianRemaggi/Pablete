package com.instituto.siga.repository;

import com.instituto.siga.entity.EstadoInscripcion;
import com.instituto.siga.entity.IntentoFinal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IntentoFinalRepository extends JpaRepository<IntentoFinal, Long> {

    /** @Query explicito por el mismo motivo que InscripcionFinalRepository (Alumno.usuarioId). */
    @Query("SELECT COUNT(i) FROM IntentoFinal i WHERE i.alumno.usuarioId = :alumnoId AND i.materia.id = :materiaId "
            + "AND i.inscripcionFinal.estado IN :estados")
    long contarActivosPorAlumnoYMateria(@Param("alumnoId") Long alumnoId, @Param("materiaId") Long materiaId,
                                         @Param("estados") List<EstadoInscripcion> estados);

    List<IntentoFinal> findByInscripcionFinalId(Long inscripcionFinalId);

    /** Otro intento del mismo alumno y materia ya aprobado: sostiene la materia APROBADA al corregir un resultado. */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM IntentoFinal i "
            + "WHERE i.alumno.usuarioId = :alumnoId AND i.materia.id = :materiaId AND i.id <> :excluirId "
            + "AND i.resultado = com.instituto.siga.entity.ResultadoFinal.APROBADO")
    boolean existeOtroIntentoAprobado(@Param("alumnoId") Long alumnoId, @Param("materiaId") Long materiaId,
                                      @Param("excluirId") Long excluirId);
}
