package com.instituto.siga.repository;

import com.instituto.siga.entity.EventoAcademico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventoAcademicoRepository extends JpaRepository<EventoAcademico, Long> {

    @Query("SELECT e FROM EventoAcademico e WHERE e.fechaInicio BETWEEN :desde AND :hasta "
            + "OR (e.fechaFin IS NOT NULL AND e.fechaFin BETWEEN :desde AND :hasta)")
    List<EventoAcademico> buscarEnRango(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
