package com.instituto.siga.repository;

import com.instituto.siga.entity.Carrera;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarreraRepository extends JpaRepository<Carrera, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByNombre(String nombre);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    boolean existsByNombreAndIdNot(String nombre, Long id);
}
