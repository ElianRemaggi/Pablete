package com.instituto.siga.service.interfaces;

public interface ExportacionService {

    byte[] exportarInscripcionesCursada(Long comisionId);

    byte[] exportarInscripcionesFinal(Long mesaId);

    byte[] exportarAlumnos(String legajo, String dni, String apellido);
}
