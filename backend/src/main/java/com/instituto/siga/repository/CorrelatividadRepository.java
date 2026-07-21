package com.instituto.siga.repository;

import com.instituto.siga.entity.Correlatividad;
import com.instituto.siga.entity.TipoRequisito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CorrelatividadRepository extends JpaRepository<Correlatividad, Long> {

    List<Correlatividad> findByMateriaId(Long materiaId);

    boolean existsByMateriaIdAndMateriaRequeridaIdAndTipoRequisito(
            Long materiaId, Long materiaRequeridaId, TipoRequisito tipoRequisito);

    /** Pares (materiaId, materiaRequeridaId) de todo el grafo, para deteccion de ciclos sin hidratar entidades. */
    @Query("SELECT c.materia.id, c.materiaRequerida.id FROM Correlatividad c")
    List<Object[]> buscarAristas();
}
