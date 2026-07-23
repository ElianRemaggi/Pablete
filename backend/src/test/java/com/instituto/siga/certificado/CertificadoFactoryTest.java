package com.instituto.siga.certificado;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.EstadoAcademicoMateria;
import com.instituto.siga.entity.EstadoMateria;
import com.instituto.siga.entity.Materia;
import com.instituto.siga.entity.Usuario;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CertificadoFactoryTest {

    private final CertificadoFactory certificadoFactory = new CertificadoFactory();

    private Alumno alumnoDeEjemplo() {
        Usuario usuario = Usuario.builder().id(1L).dni("30111111").nombre("Juan").apellido("Perez").build();
        return Alumno.builder().usuarioId(1L).usuario(usuario).legajo("LEG-0001").fechaIngreso(LocalDate.now()).build();
    }

    @Test
    void generarAlumnoRegular_devuelveUnPdfValidoYNoVacio() {
        byte[] pdf = certificadoFactory.generarAlumnoRegular(alumnoDeEjemplo());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void generarAnaliticoParcial_devuelveUnPdfValidoConHistorial() {
        Materia materia = Materia.builder().id(1L).codigo("MAT1").nombre("Materia 1").build();
        EstadoAcademicoMateria estado = EstadoAcademicoMateria.builder()
                .id(1L).alumno(alumnoDeEjemplo()).materia(materia).estado(EstadoMateria.APROBADA)
                .fechaActualizacion(LocalDateTime.now()).build();

        byte[] pdf = certificadoFactory.generarAnaliticoParcial(alumnoDeEjemplo(), List.of(estado));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }
}
