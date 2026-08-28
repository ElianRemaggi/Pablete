package com.instituto.siga.repository;

import com.instituto.siga.entity.Alumno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlumnoRepository extends JpaRepository<Alumno, Long> {

    @Query("SELECT a FROM Alumno a JOIN a.usuario u WHERE "
            + "(:legajo IS NULL OR LOWER(TRIM(a.legajo)) = LOWER(TRIM(:legajo))) "
            + "AND (:dni IS NULL OR LOWER(TRIM(u.dni)) = LOWER(TRIM(:dni))) "
            + "AND (:apellido IS NULL OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', TRIM(:apellido), '%'))) "
            + "AND (:nombre IS NULL OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', TRIM(:nombre), '%')))")
    List<Alumno> buscar(@Param("legajo") String legajo, @Param("dni") String dni, @Param("apellido") String apellido,
                         @Param("nombre") String nombre);
}
