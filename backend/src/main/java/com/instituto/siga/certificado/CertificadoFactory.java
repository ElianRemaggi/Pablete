package com.instituto.siga.certificado;

import com.instituto.siga.entity.Alumno;
import com.instituto.siga.entity.EstadoAcademicoMateria;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Factory (docs/01 SS9): un metodo por tipo de certificado, arma el PDF y devuelve los bytes. */
@Component
public class CertificadoFactory {

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font TEXTO = FontFactory.getFont(FontFactory.HELVETICA, 12);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color COLOR_INSTITUCIONAL = new Color(5, 172, 236);
    private static final Font TITULO_COMPROBANTE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Color.WHITE);
    private static final Font SUBTITULO_INSTITUTO = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.WHITE);
    private static final Font ETIQUETA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font VALOR = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font PIE = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
    private static final String NOMBRE_INSTITUTO = "Instituto Superior de Estudios Técnicos (ISET)";

    private static final float ALTO_PAGINA = PageSize.LETTER.getHeight();
    /** Tamano de letra e interlineado medidos sobre el modelo (hoja carta, 612 x 792 pt). */
    private static final float TAMANO = 9.3f;
    private static final float INTERLINEADO = 21.7f;

    private static String puntos(int cantidad) {
        return ".".repeat(cantidad);
    }

    /**
     * "Constancia de alumno regular" EN BLANCO con el formato institucional del modelo "ALUMNO REGULAR ISET.pdf"
     * (marco, logos de la Municipalidad de General Pueyrredon e ISET, texto, sello y firma). Los datos del alumno, la
     * carrera, el curso y la fecha quedan como lineas punteadas: los completa a mano el alumno o la persona autorizada.
     * Las medidas estan tomadas del modelo (hoja carta, 612 x 792 pt; coordenadas en puntos desde el borde superior).
     */
    public byte[] generarConstanciaAlumnoRegular() {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.LETTER, 0, 0, 0, 0);
        try {
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            documento.open();
            PdfContentByte lienzo = writer.getDirectContent();

            lienzo.setLineWidth(0.8f);
            lienzo.rectangle(55.7f, ALTO_PAGINA - 399f, 499.3f, 372.7f);
            lienzo.stroke();

            agregarImagen(documento, "certificado/cabecera-mgp.png", 68.5f, 47.7f, 274.2f);
            agregarImagen(documento, "certificado/logo-iset-constancia.png", 445.5f, 33.7f, 102.2f);

            BaseFont base = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
            BaseFont negrita = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
            lienzo.beginText();
            lienzo.setFontAndSize(negrita, TAMANO);
            lienzo.showTextAligned(Element.ALIGN_CENTER, "CONSTANCIA DE ALUMNO REGULAR", 305.4f, ALTO_PAGINA - 152f, 0);
            lienzo.endText();

            Font normal = new Font(base, TAMANO);
            Paragraph primero = new Paragraph(INTERLINEADO);
            primero.setAlignment(Element.ALIGN_JUSTIFIED);
            primero.add(new Chunk("Se deja constancia que, " + puntos(118) + " DNI " + puntos(20)
                    + " es alumno/a regular del Instituto Superior de Estudios Técnicos, de la carrera "
                    + puntos(95) + " curso " + puntos(22), normal));

            Paragraph segundo = new Paragraph(INTERLINEADO);
            segundo.setAlignment(Element.ALIGN_JUSTIFIED);
            segundo.add(new Chunk("A pedido del interesado/a y para ser presentada ante quien corresponda, se extiende "
                    + "la presente en la ciudad de Mar del Plata, a los " + puntos(14) + " días del mes " + puntos(24)
                    + " de " + puntos(16), normal));

            ColumnText texto = new ColumnText(lienzo);
            texto.setSimpleColumn(68.5f, ALTO_PAGINA - 300f, 543f, ALTO_PAGINA - 163.5f);
            texto.addElement(primero);
            texto.addElement(segundo);
            texto.go();

            lienzo.beginText();
            lienzo.setFontAndSize(base, TAMANO);
            lienzo.showTextAligned(Element.ALIGN_CENTER, "Sello del establecimiento", 161.6f, ALTO_PAGINA - 348.5f, 0);
            lienzo.showTextAligned(Element.ALIGN_CENTER, puntos(49), 458.4f, ALTO_PAGINA - 338.5f, 0);
            lienzo.showTextAligned(Element.ALIGN_CENTER, "Firma y sello aclaratorio del", 458.4f, ALTO_PAGINA - 349f, 0);
            lienzo.showTextAligned(Element.ALIGN_CENTER, "Director / Secretario", 458.4f, ALTO_PAGINA - 359.5f, 0);
            lienzo.endText();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("No se pudo generar la constancia de alumno regular", e);
        } finally {
            documento.close();
        }
        return salida.toByteArray();
    }

    /** Coloca un recurso PNG con su esquina superior izquierda en (x, yDesdeArriba) y el ancho indicado. */
    private void agregarImagen(Document documento, String recurso, float x, float yDesdeArriba, float ancho)
            throws DocumentException, IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(recurso)) {
            if (in == null) {
                throw new IOException("Falta el recurso " + recurso);
            }
            Image imagen = Image.getInstance(in.readAllBytes());
            imagen.scaleAbsolute(ancho, ancho * imagen.getHeight() / imagen.getWidth());
            imagen.setAbsolutePosition(x, ALTO_PAGINA - yDesdeArriba - imagen.getScaledHeight());
            documento.add(imagen);
        }
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

    /** docs/04 seccion PDF de inscripciones: comprobante de inscripcion a final. Refleja el estado real:
     * una inscripcion dada de baja (voluntaria o por mesa cancelada) se emite como tal, no como vigente. */
    public byte[] generarComprobanteFinal(Alumno alumno, String numeroComprobante, String carreraNombre,
            String materiaNombre, LocalDate fechaMesa, LocalTime hora, String turno, String periodo, String aula,
            String modalidad, String profesor, String estadoMesa, String estadoInscripcion,
            LocalDateTime fechaInscripcion) {
        List<String[]> filas = List.of(
                new String[] { "Fecha de la mesa", fechaMesa.format(FECHA) },
                new String[] { "Hora", hora == null ? null : hora.format(HORA) },
                new String[] { "Turno", turno },
                new String[] { "Período académico", periodo },
                new String[] { "Aula / sede", aula },
                new String[] { "Modalidad", modalidad },
                new String[] { "Profesor", profesor },
                new String[] { "Estado de la mesa", estadoMesa },
                new String[] { "Estado de la inscripción", estadoInscripcion });
        boolean baja = "BAJA".equals(estadoInscripcion);
        return generarComprobante(baja ? "Comprobante de Inscripción a Final (DADA DE BAJA)" : "Comprobante de Inscripción a Final",
                numeroComprobante, alumno, carreraNombre, materiaNombre, filas, fechaInscripcion,
                baja ? "tuvo una inscripción, actualmente dada de baja, al examen final de la asignatura"
                        : "se encuentra inscripto/a al examen final de la asignatura");
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
