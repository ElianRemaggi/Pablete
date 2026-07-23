package com.instituto.siga.repository;

import com.instituto.siga.entity.PlanEstudio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanEstudioRepository extends JpaRepository<PlanEstudio, Long> {

    List<PlanEstudio> findByCarreraId(Long carreraId);

    boolean existsByCarreraIdAndVersion(Long carreraId, String version);

    boolean existsByCarreraIdAndVersionAndIdNot(Long carreraId, String version, Long id);
}
