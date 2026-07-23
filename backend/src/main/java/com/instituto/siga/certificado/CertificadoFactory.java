package com.instituto.siga.certificado;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.EstadoAcademicoMateria;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Factory (docs/01 SS9): un metodo por tipo de certificado, arma el PDF y devuelve los bytes. */
@Component
public class CertificadoFactory {

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font TEXTO = FontFactory.getFont(FontFactory.HELVETICA, 12);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generarAlumnoRegular(Alumno alumno) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document();
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();
            documento.add(new Paragraph("Certificado de Alumno Regular", TITULO));
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Se certifica que " + alumno.getUsuario().getNombre() + " "
                    + alumno.getUsuario().getApellido() + " (DNI " + alumno.getUsuario().getDni()
                    + ", legajo " + alumno.getLegajo() + ") reviste la condicion de alumno regular"
                    + " de esta institucion.", TEXTO));
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Fecha de emision: " + LocalDate.now().format(FECHA), TEXTO));
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el certificado de alumno regular", e);
        } finally {
            documento.close();
        }
        return salida.toByteArray();
    }

    public byte[] generarAnaliticoParcial(Alumno alumno, List<EstadoAcademicoMateria> historial) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document();
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();
            documento.add(new Paragraph("Certificado Analitico Parcial", TITULO));
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph(alumno.getUsuario().getNombre() + " " + alumno.getUsuario().getApellido()
                    + " (DNI " + alumno.getUsuario().getDni() + ", legajo " + alumno.getLegajo() + ")", TEXTO));
            documento.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(3);
            tabla.addCell("Materia");
            tabla.addCell("Estado");
            tabla.addCell("Fecha");
            for (EstadoAcademicoMateria estado : historial) {
                tabla.addCell(estado.getMateria().getNombre());
                tabla.addCell(estado.getEstado().name());
                tabla.addCell(estado.getFechaActualizacion().format(FECHA_HORA));
            }
            documento.add(tabla);
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Fecha de emision: " + LocalDate.now().format(FECHA), TEXTO));
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el certificado analitico parcial", e);
        } finally {
            documento.close();
        }
        return salida.toByteArray();
    }
}
