package com.instituto.siga.repository;

import com.instituto.siga.entity.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlumnoRepository extends JpaRepository<Alumno, Long> {

    @Query("SELECT a FROM Alumno a JOIN a.usuario u WHERE "
            + "(:legajo IS NULL OR a.legajo = :legajo) "
            + "AND (:dni IS NULL OR u.dni = :dni) "
            + "AND (:apellido IS NULL OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', :apellido, '%')))")
    List<Alumno> buscar(@Param("legajo") String legajo, @Param("dni") String dni, @Param("apellido") String apellido);
}
