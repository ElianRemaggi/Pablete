package com.instituto.siga.dto.response;

import java.util.List;

/**
 * Resultado de importar (o previsualizar) un historial academico subido desde la interfaz. La vista previa y la
 * ejecucion usan exactamente la misma logica: {@code confirmada=false} describe lo que pasaria sin tocar la base.
 * Los contadores de "registros" son por (alumno, materia).
 */
public record ImportacionHistorialResponse(
        boolean confirmada,
        String archivo,
        int hojas,
        int filasLeidas,
        int alumnosExistentes,
        int alumnosNuevos,
        int registrosCreados,
        int registrosActualizados,
        int registrosSinCambios,
        int registrosEnConflicto,
        int registrosRechazados,
        int filasRechazadas,
        /** Primeros cambios/conflictos (hasta el tope); {@code hayMasCambios} avisa si se recortaron. */
        List<Cambio> cambios,
        boolean hayMasCambios,
        /** Todas las filas/celdas rechazadas, con su motivo (base del reporte de errores descargable). */
        List<Error> errores,
        List<String> advertencias) {

    /** tipo: CREAR, ACTUALIZAR o CONFLICTO (conflicto = se conserva la base). */
    public record Cambio(String hoja, String dni, String alumno, String materia, String tipo,
                         String estadoActual, String estadoNuevo, Integer notaActual, Integer notaNueva, String detalle) {
    }

    public record Error(String hoja, String dni, String materia, String motivo) {
    }
}
