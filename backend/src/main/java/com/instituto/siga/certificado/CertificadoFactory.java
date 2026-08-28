package com.instituto.siga.certificado;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.EstadoAcademicoMateria;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Rectangle;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Factory (docs/01 SS9): un metodo por tipo de certificado, arma el PDF y devuelve los bytes. */
@Component
public class CertificadoFactory {

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font TEXTO = FontFactory.getFont(FontFactory.HELVETICA, 12);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color COLOR_INSTITUCIONAL = new Color(5, 172, 236);
    private static final Font TITULO_COMPROBANTE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Color.WHITE);
    private static final Font SUBTITULO_INSTITUTO = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.WHITE);
    private static final Font ETIQUETA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font VALOR = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font PIE = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
    private static final String NOMBRE_INSTITUTO = "Instituto Superior de Estudios Técnicos (ISET)";

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

    /** docs/04 seccion PDF de inscripciones: comprobante de inscripcion a cursada. */
    public byte[] generarComprobanteCursada(Alumno alumno, String numeroComprobante, String carreraNombre,
            String materiaNombre, String comisionNombre, String profesor, LocalDateTime fechaInscripcion) {
        List<String[]> filas = List.of(
                new String[] { "Comisión", comisionNombre },
                new String[] { "Profesor", profesor == null ? "—" : profesor });
        return generarComprobante("Comprobante de Inscripción a Cursada", numeroComprobante, alumno, carreraNombre,
                materiaNombre, filas, fechaInscripcion,
                "se encuentra inscripto/a a la cursada de la asignatura");
    }

    /** docs/04 seccion PDF de inscripciones: comprobante de inscripcion a final. */
    public byte[] generarComprobanteFinal(Alumno alumno, String numeroComprobante, String carreraNombre,
            String materiaNombre, LocalDate fechaMesa, String turno, String profesor, LocalDateTime fechaInscripcion) {
        List<String[]> filas = List.of(
                new String[] { "Fecha de la mesa", fechaMesa.format(FECHA) },
                new String[] { "Turno", turno },
                new String[] { "Profesor", profesor == null ? "—" : profesor });
        return generarComprobante("Comprobante de Inscripción a Final", numeroComprobante, alumno, carreraNombre,
                materiaNombre, filas, fechaInscripcion,
                "se encuentra inscripto/a al examen final de la asignatura");
    }

    private byte[] generarComprobante(String tituloDocumento, String numeroComprobante, Alumno alumno,
            String carreraNombre, String materiaNombre, List<String[]> filasEspecificas,
            LocalDateTime fechaInscripcion, String verboInscripcion) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 50, 50, 0, 60);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            agregarEncabezado(documento, tituloDocumento);

            Paragraph cuerpo = new Paragraph(
                    "El alumno/a " + alumno.getUsuario().getNombre() + " " + alumno.getUsuario().getApellido()
                            + ", DNI " + alumno.getUsuario().getDni() + " (legajo " + alumno.getLegajo() + "), "
                            + verboInscripcion + " " + materiaNombre
                            + (carreraNombre == null ? "" : ", correspondiente a la carrera " + carreraNombre)
                            + ", con fecha de inscripción " + fechaInscripcion.format(FECHA_HORA) + ".",
                    TEXTO);
            cuerpo.setSpacingBefore(20);
            cuerpo.setSpacingAfter(20);
            documento.add(cuerpo);

            PdfPTable tabla = new PdfPTable(2);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[] { 1f, 2f });
            agregarFila(tabla, "Alumno/a", alumno.getUsuario().getNombre() + " " + alumno.getUsuario().getApellido());
            agregarFila(tabla, "DNI", alumno.getUsuario().getDni());
            agregarFila(tabla, "Legajo", alumno.getLegajo());
            if (carreraNombre != null) {
                agregarFila(tabla, "Carrera", carreraNombre);
            }
            agregarFila(tabla, "Materia", materiaNombre);
            for (String[] fila : filasEspecificas) {
                agregarFila(tabla, fila[0], fila[1]);
            }
            agregarFila(tabla, "Fecha de inscripción", fechaInscripcion.format(FECHA_HORA));
            agregarFila(tabla, "Comprobante generado el", LocalDateTime.now().format(FECHA_HORA));
            agregarFila(tabla, "N° de comprobante", numeroComprobante);
            documento.add(tabla);

            Paragraph pie = new Paragraph(
                    "Este comprobante es de carácter informativo y puede utilizarse como constancia de inscripción. "
                            + NOMBRE_INSTITUTO + " · N° " + numeroComprobante,
                    PIE);
            pie.setSpacingBefore(30);
            pie.setAlignment(Element.ALIGN_CENTER);
            documento.add(pie);
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el comprobante de inscripción", e);
        } finally {
            documento.close();
        }
        return salida.toByteArray();
    }

    private void agregarEncabezado(Document documento, String tituloDocumento) throws DocumentException {
        PdfPTable banda = new PdfPTable(2);
        banda.setWidthPercentage(100);
        banda.setWidths(new float[] { 1f, 4f });

        PdfPCell celdaLogo = new PdfPCell();
        celdaLogo.setBackgroundColor(COLOR_INSTITUCIONAL);
        celdaLogo.setBorder(Rectangle.NO_BORDER);
        celdaLogo.setPadding(10);
        Image logo = cargarLogo();
        if (logo != null) {
            logo.scaleToFit(60, 60);
            celdaLogo.addElement(logo);
        }
        banda.addCell(celdaLogo);

        PdfPCell celdaTitulo = new PdfPCell();
        celdaTitulo.setBackgroundColor(COLOR_INSTITUCIONAL);
        celdaTitulo.setBorder(Rectangle.NO_BORDER);
        celdaTitulo.setPadding(10);
        celdaTitulo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph instituto = new Paragraph(NOMBRE_INSTITUTO, SUBTITULO_INSTITUTO);
        Paragraph titulo = new Paragraph(tituloDocumento, TITULO_COMPROBANTE);
        titulo.setSpacingBefore(4);
        celdaTitulo.addElement(instituto);
        celdaTitulo.addElement(titulo);
        banda.addCell(celdaTitulo);

        documento.add(banda);
        documento.add(new Chunk(" "));
    }

    private void agregarFila(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new com.lowagie.text.Phrase(etiqueta, ETIQUETA));
        celdaEtiqueta.setBorder(Rectangle.BOTTOM);
        celdaEtiqueta.setPadding(6);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new com.lowagie.text.Phrase(valor == null ? "—" : valor, VALOR));
        celdaValor.setBorder(Rectangle.BOTTOM);
        celdaValor.setPadding(6);
        tabla.addCell(celdaValor);
    }

    private Image cargarLogo() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("logo-iset.jpg")) {
            if (in == null) {
                return null;
            }
            return Image.getInstance(in.readAllBytes());
        } catch (IOException | BadElementException e) {
            return null;
        }
    }
}
