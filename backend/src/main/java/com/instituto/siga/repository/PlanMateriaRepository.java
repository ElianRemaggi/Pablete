package com.instituto.siga.repository;

import com.instituto.siga.entity.PlanMateria;
import com.instituto.siga.entity.PlanMateriaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlanMateriaRepository extends JpaRepository<PlanMateria, PlanMateriaId> {

    List<PlanMateria> findByPlanId(Long planId);

    List<PlanMateria> findByMateriaId(Long materiaId);

    boolean existsByPlanIdAndMateriaId(Long planId, Long materiaId);

    /** El alumno no esta vinculado a una carrera en JPA: se resuelve por Materia -> Plan -> Carrera,
     * prefiriendo el plan vigente si la materia esta asociada a mas de uno. */
    @Query("SELECT c.nombre FROM PlanMateria pm JOIN pm.plan p JOIN p.carrera c "
            + "WHERE pm.materia.id = :materiaId ORDER BY p.vigente DESC")
    List<String> buscarNombresCarreraPorMateria(@Param("materiaId") Long materiaId);

    default Optional<String> buscarCarreraVigentePorMateria(Long materiaId) {
        List<String> nombres = buscarNombresCarreraPorMateria(materiaId);
        return nombres.isEmpty() ? Optional.empty() : Optional.of(nombres.get(0));
    }
}
