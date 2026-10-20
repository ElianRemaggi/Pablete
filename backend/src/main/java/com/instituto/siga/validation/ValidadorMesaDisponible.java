package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.EstadoMesa;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.exception.BusinessException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Solo las mesas PUBLICADAS admiten inscripciones (alta y cambio de mesa). */
@Component
@Order(0)
public class ValidadorMesaDisponible implements ValidadorInscripcionFinal {

    @Override
    public void validar(Alumno alumno, MesaExamen mesa, InscripcionFinal inscripcionExistente) {
        EstadoMesa estado = mesa.getEstado();
        if (estado == EstadoMesa.PUBLICADA) {
            return;
        }
        String motivo = switch (estado) {
            case BORRADOR -> "La mesa todavía no fue publicada";
            case CERRADA -> "La inscripción a esta mesa está cerrada";
            case CANCELADA -> "La mesa fue cancelada";
            case PUBLICADA -> throw new IllegalStateException("inalcanzable");
        };
        throw new BusinessException("MESA_NO_DISPONIBLE", motivo);
    }
}
