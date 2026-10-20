package com.instituto.siga.validation;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.InscripcionFinal;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Regla 5 de docs/07-reglas-negocio-validaciones.md SS2. Solo se corre en el alta (ver SS2 "Modificación/baja").
 * Usa la ventana propia de la mesa si existe; si no, la general (ver {@link VentanaInscripcionMesa}). */
@Component
@Order(5)
@RequiredArgsConstructor
public class ValidadorVentanaInscripcionFinal implements ValidadorInscripcionFinal {

    private final VentanaInscripcionMesa ventanaInscripcionMesa;

    @Override
    public void validar(Alumno alumno, MesaExamen mesa, InscripcionFinal inscripcionExistente) {
        if (!ventanaInscripcionMesa.estaAbierta(mesa, LocalDateTime.now())) {
            throw new BusinessException("FUERA_DE_PLAZO", ventanaInscripcionMesa.tieneVentanaPropia(mesa)
                    ? "Fuera del período de inscripción de esta mesa"
                    : "Fuera del período de inscripción a finales");
        }
    }
}
