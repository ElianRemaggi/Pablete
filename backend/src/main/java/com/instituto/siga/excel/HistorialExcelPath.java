package com.instituto.siga.excel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resuelve la ruta efectiva del .xlsx del historial. Una ruta absoluta (p. ej. via la variable de
 * entorno HISTORIAL_EXCEL_PATH) se respeta tal cual. Una ruta relativa (el default es
 * {@code ./data/historial-academico.xlsx}) NO se resuelve contra el directorio de trabajo a ciegas:
 * segun se arranque el backend (desde {@code backend/} con mvnw, o desde la raiz del proyecto) ese
 * directorio cambia, y el archivo "desaparecia". Si el directorio de trabajo es la raiz del proyecto
 * o la carpeta {@code backend/}, la ruta relativa se ancla a la raiz del proyecto (la carpeta que
 * contiene {@code backend/pom.xml}); en cualquier otro caso, al directorio de trabajo.
 */
@Component
public class HistorialExcelPath {

    private final String configurada;
    private final Path directorioTrabajo;

    @Autowired
    public HistorialExcelPath(@Value("${app.historial.excel-path}") String configurada) {
        this(configurada, Path.of("").toAbsolutePath());
    }

    HistorialExcelPath(String configurada, Path directorioTrabajo) {
        this.configurada = configurada;
        this.directorioTrabajo = directorioTrabajo.toAbsolutePath().normalize();
    }

    public Path resolver() {
        Path ruta = Path.of(configurada);
        if (ruta.isAbsolute()) {
            return ruta.normalize();
        }
        return base().resolve(ruta).normalize();
    }

    private Path base() {
        if (esRaizDelProyecto(directorioTrabajo)) {
            return directorioTrabajo;
        }
        Path padre = directorioTrabajo.getParent();
        if (padre != null && esRaizDelProyecto(padre)
                && directorioTrabajo.getFileName().toString().equals("backend")) {
            return padre;
        }
        return directorioTrabajo;
    }

    private boolean esRaizDelProyecto(Path directorio) {
        return Files.exists(directorio.resolve("backend").resolve("pom.xml"));
    }
}
