import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, Chip, CircularProgress, MenuItem, Stack, TextField, Typography } from '@mui/material';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { FileDownloadButton } from '../../components/FileDownloadButton';
import { obtenerHistorialPropio, type HistorialAcademicoResponse } from '../../api/alumnosApi';

const ESTADOS = ['TODOS', 'PENDIENTE', 'CURSANDO', 'REGULAR', 'APROBADA', 'LIBRE'] as const;

export function HistorialAcademicoPage() {
  const { data, isLoading, isError } = useQuery({ queryKey: ['alumno', 'historial'], queryFn: obtenerHistorialPropio });
  const [estado, setEstado] = useState<(typeof ESTADOS)[number]>('TODOS');

  const filtrado = useMemo(() => {
    if (!data) return [];
    return estado === 'TODOS' ? data : data.filter((h) => h.estado === estado);
  }, [data, estado]);

  const columnas: DataTableColumn<HistorialAcademicoResponse>[] = [
    { key: 'materiaCodigo', header: 'Código', render: (h) => h.materiaCodigo },
    { key: 'materiaNombre', header: 'Materia', render: (h) => h.materiaNombre },
    { key: 'estado', header: 'Estado', render: (h) => <Chip label={h.estado} size="small" /> },
    { key: 'nota', header: 'Nota', render: (h) => h.nota ?? '—' },
    {
      key: 'fecha',
      header: 'Última actualización',
      render: (h) => new Date(h.fechaActualizacion).toLocaleString('es-AR'),
    },
  ];

  return (
    <Stack spacing={3}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap' }}>
        <Typography variant="h4">Historial académico</Typography>
        <FileDownloadButton
          url="/certificados/analitico-parcial"
          nombreArchivo="analitico-parcial.pdf"
          label="Descargar analítico parcial (PDF)"
          variant="outlined"
        />
      </Stack>

      <TextField
        select
        label="Estado"
        value={estado}
        onChange={(e) => setEstado(e.target.value as (typeof ESTADOS)[number])}
        sx={{ maxWidth: 250 }}
      >
        {ESTADOS.map((e) => (
          <MenuItem key={e} value={e}>
            {e}
          </MenuItem>
        ))}
      </TextField>

      {isLoading && <CircularProgress />}
      {isError && <Alert severity="error">No se pudo cargar el historial académico.</Alert>}
      {data && filtrado.length === 0 && <Alert severity="info">No hay materias con ese estado.</Alert>}
      {data && filtrado.length > 0 && <DataTable columns={columnas} rows={filtrado} getRowKey={(h) => h.materiaCodigo} />}
    </Stack>
  );
}
