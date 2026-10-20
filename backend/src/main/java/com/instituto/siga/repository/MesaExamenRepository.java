package com.instituto.siga.repository;

import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.entity.Turno;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MesaExamenRepository extends JpaRepository<MesaExamen, Long> {

    /** Lock pesimista de escritura: serializa las inscripciones concurrentes a una misma mesa para
     * que la validacion de cupo no se pueda saltear con solicitudes simultaneas. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MesaExamen m WHERE m.id = :id")
    Optional<MesaExamen> findByIdForUpdate(@Param("id") Long id);

    List<MesaExamen> findByMateriaId(Long materiaId);

    List<MesaExamen> findByFecha(LocalDate fecha);

    List<MesaExamen> findByMateriaIdAndFecha(Long materiaId, LocalDate fecha);

    boolean existsByMateriaIdAndFechaAndTurno(Long materiaId, LocalDate fecha, Turno turno);

    boolean existsByMateriaIdAndFechaAndTurnoAndIdNot(Long materiaId, LocalDate fecha, Turno turno, Long id);
}
