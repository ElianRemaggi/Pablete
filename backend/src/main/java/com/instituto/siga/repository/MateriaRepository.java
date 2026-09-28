package com.instituto.siga.repository;

import com.instituto.siga.entity.Materia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MateriaRepository extends JpaRepository<Materia, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    /** Usado por la importacion de historial desde Excel para matchear el nombre de materia
     * de cada columna contra el catalogo ya cargado (docs/04, capa excel/). Lista en vez de
     * Optional a proposito: nombre no es unique en la entidad, y un match ambiguo (>1) debe
     * reportarse en vez de importar contra la materia equivocada. */
    List<Materia> findByNombreIgnoreCase(String nombre);

    /**
     * LEFT JOIN a proposito: sin filtros debe devolver TODAS las materias, incluidas las que
     * todavia no estan asociadas a ningun plan (docs/04 SS5 preve los filtros, pero el listado
     * sin filtros no debe excluir nada).
     */
    @Query("SELECT DISTINCT m FROM Materia m "
            + "LEFT JOIN PlanMateria pm ON pm.materia = m "
            + "LEFT JOIN pm.plan p "
            + "LEFT JOIN p.carrera c "
            + "WHERE (:carreraId IS NULL OR c.id = :carreraId) "
            + "AND (:planId IS NULL OR p.id = :planId) "
            + "AND (:anio IS NULL OR pm.anio = :anio)")
    List<Materia> buscar(@Param("carreraId") Long carreraId, @Param("planId") Long planId, @Param("anio") Integer anio);
}
