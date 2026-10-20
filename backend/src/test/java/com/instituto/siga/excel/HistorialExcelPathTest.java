package com.instituto.siga.excel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** La ruta relativa por defecto no debe depender de desde que carpeta se arranque el backend. */
class HistorialExcelPathTest {

    private static final String POR_DEFECTO = "./data/historial-academico.xlsx";

    private Path crearProyecto(Path raiz) throws IOException {
        Files.createDirectories(raiz.resolve("backend"));
        Files.writeString(raiz.resolve("backend").resolve("pom.xml"), "<project/>");
        return raiz;
    }

    @Test
    void desdeLaRaizDelProyecto_seAnclaALaRaiz(@TempDir Path tmp) throws IOException {
        Path raiz = crearProyecto(tmp.resolve("SIGA"));
        assertThat(new HistorialExcelPath(POR_DEFECTO, raiz).resolver())
                .isEqualTo(raiz.resolve("data").resolve("historial-academico.xlsx").toAbsolutePath().normalize());
    }

    @Test
    void desdeBackend_seAnclaALaRaizYNoAbackendData(@TempDir Path tmp) throws IOException {
        Path raiz = crearProyecto(tmp.resolve("SIGA"));
        Path resuelta = new HistorialExcelPath(POR_DEFECTO, raiz.resolve("backend")).resolver();
        assertThat(resuelta).isEqualTo(raiz.resolve("data").resolve("historial-academico.xlsx").toAbsolutePath().normalize());
        assertThat(resuelta.toString()).doesNotContain("backend");
    }

    @Test
    void desdeCualquierOtraCarpeta_seUsaElDirectorioDeTrabajo(@TempDir Path tmp) {
        assertThat(new HistorialExcelPath(POR_DEFECTO, tmp).resolver())
                .isEqualTo(tmp.resolve("data").resolve("historial-academico.xlsx").toAbsolutePath().normalize());
    }

    @Test
    void unaRutaAbsoluta_sinDudasSeRespeta(@TempDir Path tmp) throws IOException {
        Path raiz = crearProyecto(tmp.resolve("SIGA"));
        Path absoluta = tmp.resolve("otro-lugar").resolve("mi-historial.xlsx").toAbsolutePath();
        assertThat(new HistorialExcelPath(absoluta.toString(), raiz).resolver()).isEqualTo(absoluta.normalize());
    }
}
