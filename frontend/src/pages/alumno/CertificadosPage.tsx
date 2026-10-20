import { useQuery } from '@tanstack/react-query';
import { Alert, CircularProgress, Stack, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { EstadoChip } from '../../components/EstadoChip';
import { FileDownloadButton } from '../../components/FileDownloadButton';
import { formatearFecha } from '../../utils/fechas';
import { listarHistorialCertificados, type CertificadoResponse } from '../../api/certificadosApi';
import { listarInscripcionesFinalPropias, type InscripcionFinalResponse } from '../../api/inscripcionFinalApi';

export function CertificadosPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['certificados', 'historial'],
    queryFn: listarHistorialCertificados,
  });
  const inscripcionesQuery = useQuery({
    queryKey: ['inscripciones-final', 'me'],
    queryFn: listarInscripcionesFinalPropias,
  });

  const columnas: DataTableColumn<CertificadoResponse>[] = [
    { key: 'tipo', header: 'Tipo', render: (c) => c.tipo },
    { key: 'fecha', header: 'Fecha de emisión', render: (c) => new Date(c.fechaEmision).toLocaleString('es-AR') },
  ];

  const columnasComprobantes: DataTableColumn<InscripcionFinalResponse>[] = [
    { key: 'materia', header: 'Materia', render: (i) => i.materiaNombre },
    { key: 'fecha', header: 'Fecha del examen', render: (i) => formatearFecha(i.fechaMesa) },
    { key: 'estado', header: 'Estado', render: (i) => <EstadoChip estado={i.estado} /> },
    {
      key: 'pdf',
      header: 'Comprobante',
      render: (i) => (
        <FileDownloadButton
          url={`/certificados/inscripcion-final/${i.id}`}
          nombreArchivo={`comprobante-final-${i.id}.pdf`}
          label="Descargar PDF"
          size="small"
        />
      ),
    },
  ];

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Comprobantes y certificados</Typography>

      <Typography variant="h6">Comprobantes de inscripción a finales</Typography>
      {inscripcionesQuery.isLoading && <CircularProgress />}
      {inscripcionesQuery.isError && <Alert severity="error">No se pudieron cargar tus inscripciones.</Alert>}
      {inscripcionesQuery.data && inscripcionesQuery.data.length === 0 && (
        <Alert severity="info">
          Todavía no tenés inscripciones a finales. Inscribite desde <Link to="/alumno/inscripcion-final">Exámenes finales</Link>.
        </Alert>
      )}
      {inscripcionesQuery.data && inscripcionesQuery.data.length > 0 && (
        <DataTable columns={columnasComprobantes} rows={inscripcionesQuery.data} getRowKey={(i) => i.id} />
      )}

      <Typography variant="h6">Certificados</Typography>
      <Stack direction="row" spacing={2} sx={{ flexWrap: 'wrap' }}>
        <FileDownloadButton
          url="/certificados/alumno-regular"
          nombreArchivo="certificado-alumno-regular.pdf"
          label="Certificado de alumno regular"
          variant="contained"
        />
        <FileDownloadButton
          url="/certificados/analitico-parcial"
          nombreArchivo="analitico-parcial.pdf"
          label="Analítico parcial"
          variant="contained"
        />
      </Stack>

      <Typography variant="h6">Historial de documentos emitidos</Typography>
      {isLoading && <CircularProgress />}
      {isError && <Alert severity="error">No se pudo cargar el historial de certificados.</Alert>}
      {data && data.length === 0 && <Alert severity="info">Todavía no descargaste ningún documento.</Alert>}
      {data && data.length > 0 && <DataTable columns={columnas} rows={data} getRowKey={(c) => c.id} />}
    </Stack>
  );
}
