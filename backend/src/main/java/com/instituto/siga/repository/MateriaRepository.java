package com.instituto.siga.repository;

import com.instituto.siga.entity.Materia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MateriaRepository extends JpaRepository<Materia, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

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
