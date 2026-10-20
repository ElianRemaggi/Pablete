package com.instituto.siga.excel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Si el historial configurado no existe (instalacion nueva, archivo movido o borrado), lo crea VACIO a partir de
 * la plantilla empaquetada ({@code historial-academico-plantilla.xlsx}: las mismas hojas y encabezados del
 * ejemplo original, sin alumnos). Nunca pisa un archivo existente, aunque este danado: ese caso se informa y lo
 * resuelve una persona (ver docs/11-historial-excel.md, "Recuperacion").
 */
@Slf4j
@Component
public class HistorialExcelBootstrap {

    static final String PLANTILLA = "historial-academico-plantilla.xlsx";

    /** @return true si creo el archivo; false si ya existia o no pudo crearlo (el motivo queda en el log). */
    public boolean asegurarArchivo(Path archivo) {
        if (Files.exists(archivo)) {
            return false;
        }
        Path temporal = archivo.resolveSibling(archivo.getFileName() + ".nuevo");
        try (InputStream plantilla = new ClassPathResource(PLANTILLA).getInputStream()) {
            Files.createDirectories(archivo.toAbsolutePath().getParent());
            Files.copy(plantilla, temporal, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporal, archivo);
            log.info("No existia el historial academico: se creo vacio desde la plantilla en {}", archivo);
            return true;
        } catch (IOException | RuntimeException e) {
            log.error("No se pudo crear el historial academico vacio en {}", archivo, e);
            try {
                Files.deleteIfExists(temporal);
            } catch (IOException ignorada) {
                // sin accion: ya se informo el error original
            }
            return false;
        }
    }
}
