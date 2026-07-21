package com.instituto.siga.repository;

import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.entity.Turno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MesaExamenRepository extends JpaRepository<MesaExamen, Long> {

    List<MesaExamen> findByMateriaId(Long materiaId);

    List<MesaExamen> findByFecha(LocalDate fecha);

    List<MesaExamen> findByMateriaIdAndFecha(Long materiaId, LocalDate fecha);

    boolean existsByMateriaIdAndFechaAndTurno(Long materiaId, LocalDate fecha, Turno turno);

    boolean existsByMateriaIdAndFechaAndTurnoAndIdNot(Long materiaId, LocalDate fecha, Turno turno, Long id);
}
