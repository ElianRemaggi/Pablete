import { useQuery } from '@tanstack/react-query';
import { Alert, CircularProgress, Stack, Typography } from '@mui/material';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { FileDownloadButton } from '../../components/FileDownloadButton';
import { listarHistorialCertificados, type CertificadoResponse } from '../../api/certificadosApi';

export function CertificadosPage() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['certificados', 'historial'],
    queryFn: listarHistorialCertificados,
  });

  const columnas: DataTableColumn<CertificadoResponse>[] = [
    { key: 'tipo', header: 'Tipo', render: (c) => c.tipo },
    { key: 'fecha', header: 'Fecha de emisión', render: (c) => new Date(c.fechaEmision).toLocaleString('es-AR') },
  ];

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Certificados</Typography>

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

      <Typography variant="h6">Historial de certificados emitidos</Typography>
      {isLoading && <CircularProgress />}
      {isError && <Alert severity="error">No se pudo cargar el historial de certificados.</Alert>}
      {data && data.length === 0 && <Alert severity="info">Todavía no descargaste ningún certificado.</Alert>}
      {data && data.length > 0 && <DataTable columns={columnas} rows={data} getRowKey={(c) => c.id} />}
    </Stack>
  );
}
