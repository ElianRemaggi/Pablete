package com.instituto.siga.repository;

import com.instituto.siga.entity.PlanMateria;
import com.instituto.siga.entity.PlanMateriaId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanMateriaRepository extends JpaRepository<PlanMateria, PlanMateriaId> {

    List<PlanMateria> findByPlanId(Long planId);

    List<PlanMateria> findByMateriaId(Long materiaId);

    boolean existsByPlanIdAndMateriaId(Long planId, Long materiaId);
}
