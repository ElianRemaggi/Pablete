package com.instituto.siga.repository;

import com.instituto.siga.entity.Comision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComisionRepository extends JpaRepository<Comision, Long> {

    List<Comision> findByMateriaId(Long materiaId);

    List<Comision> findByPeriodoLectivo(String periodoLectivo);

    List<Comision> findByMateriaIdAndPeriodoLectivo(Long materiaId, String periodoLectivo);

    boolean existsByMateriaIdAndNombreAndPeriodoLectivo(Long materiaId, String nombre, String periodoLectivo);

    boolean existsByMateriaIdAndNombreAndPeriodoLectivoAndIdNot(
            Long materiaId, String nombre, String periodoLectivo, Long id);
}
