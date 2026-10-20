package com.instituto.siga.validation;

import com.instituto.siga.entity.ConfiguracionInstitucional;
import com.instituto.siga.entity.MesaExamen;
import com.instituto.siga.exception.ResourceNotFoundException;
import com.instituto.siga.repository.ConfiguracionInstitucionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Ventana de inscripcion efectiva de una mesa: la propia de la mesa si el administrativo la definio
 * (apertura y cierre juntos); si no, la ventana general de finales de ConfiguracionInstitucional.
 * Un unico lugar para que el validador y el listado de mesas no puedan discrepar.
 */
@Component
@RequiredArgsConstructor
public class VentanaInscripcionMesa {

    private final ConfiguracionInstitucionalRepository configuracionRepository;

    public boolean tieneVentanaPropia(MesaExamen mesa) {
        return mesa.getAperturaInscripcion() != null && mesa.getCierreInscripcion() != null;
    }

    public boolean estaAbierta(MesaExamen mesa, LocalDateTime ahora) {
        LocalDateTime apertura;
        LocalDateTime cierre;
        if (tieneVentanaPropia(mesa)) {
            apertura = mesa.getAperturaInscripcion();
            cierre = mesa.getCierreInscripcion();
        } else {
            ConfiguracionInstitucional config = configuracionRepository.findById(1)
                    .orElseThrow(() -> new ResourceNotFoundException("Configuración institucional no inicializada"));
            apertura = config.getFechaAperturaFinales();
            cierre = config.getFechaCierreFinales();
        }
        return !ahora.isBefore(apertura) && !ahora.isAfter(cierre);
    }
}
