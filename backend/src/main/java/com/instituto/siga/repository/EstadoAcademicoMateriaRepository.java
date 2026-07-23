package com.instituto.siga.repository;

import com.instituto.siga.entity.EstadoAcademicoMateria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EstadoAcademicoMateriaRepository extends JpaRepository<EstadoAcademicoMateria, Long> {

    /**
     * @Query explicito, no derivado: el @Id de Alumno se llama "usuarioId" (por @MapsId a
     * Usuario), no "id" — "findByAlumnoId..." no se puede derivar automaticamente porque
     * Spring Data busca una propiedad literal "id" en Alumno y no existe con ese nombre.
     */
    @Query("SELECT e FROM EstadoAcademicoMateria e WHERE e.alumno.usuarioId = :alumnoId AND e.materia.id = :materiaId")
    Optional<EstadoAcademicoMateria> findByAlumnoIdAndMateriaId(@Param("alumnoId") Long alumnoId, @Param("materiaId") Long materiaId);

    @Query("SELECT e FROM EstadoAcademicoMateria e WHERE e.alumno.usuarioId = :alumnoId")
    List<EstadoAcademicoMateria> findByAlumnoId(@Param("alumnoId") Long alumnoId);
}
