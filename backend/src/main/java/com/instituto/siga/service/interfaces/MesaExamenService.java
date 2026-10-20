package com.instituto.siga.service.interfaces;

import com.instituto.siga.dto.request.MesaExamenRequest;
import com.instituto.siga.dto.response.MesaExamenResponse;
import com.instituto.siga.entity.EstadoMesa;

import java.time.LocalDate;
import java.util.List;

public interface MesaExamenService {

    /** Los alumnos solo ven mesas PUBLICADAS; el personal ve todas (y puede filtrar por estado). */
    List<MesaExamenResponse> listar(Long materiaId, LocalDate fecha, EstadoMesa estado, String periodo);

    MesaExamenResponse obtener(Long id);

    MesaExamenResponse crear(MesaExamenRequest request);

    MesaExamenResponse actualizar(Long id, MesaExamenRequest request);

    /** Borrado fisico: solo para mesas en BORRADOR sin inscripciones. Las demas se cancelan. */
    void eliminar(Long id);

    MesaExamenResponse publicar(Long id);

    MesaExamenResponse cerrar(Long id);

    MesaExamenResponse cancelar(Long id);
}
