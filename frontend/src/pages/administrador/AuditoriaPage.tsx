import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Alert,
  Button,
  CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import { IconButton } from '@mui/material';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { listarAuditoria, type AuditoriaResponse } from '../../api/auditoriaApi';

function formatearDetalle(detalle: string): string {
  try {
    const parseado = JSON.parse(detalle);
    return JSON.stringify(typeof parseado === 'string' ? JSON.parse(parseado) : parseado, null, 2);
  } catch {
    return detalle;
  }
}

export function AuditoriaPage() {
  const [accion, setAccion] = useState('');
  const [entidad, setEntidad] = useState('');
  const [filtros, setFiltros] = useState({ accion: '', entidad: '' });
  const [detalleSeleccionado, setDetalleSeleccionado] = useState<AuditoriaResponse | null>(null);

  const { data, isLoading, isError } = useQuery({
    queryKey: ['auditoria', filtros],
    queryFn: () => listarAuditoria(filtros),
  });

  const columnas: DataTableColumn<AuditoriaResponse>[] = [
    { key: 'fecha', header: 'Fecha', render: (a) => new Date(a.fecha).toLocaleString('es-AR') },
    { key: 'usuario', header: 'Usuario', render: (a) => a.usuarioNombre ?? '—' },
    { key: 'accion', header: 'Acción', render: (a) => a.accion },
    { key: 'entidad', header: 'Entidad', render: (a) => `${a.entidad}${a.entidadId ? ` #${a.entidadId}` : ''}` },
    { key: 'ip', header: 'IP', render: (a) => a.ip },
    {
      key: 'detalle',
      header: 'Detalle',
      render: (a) => (
        <IconButton size="small" onClick={() => setDetalleSeleccionado(a)}>
          <VisibilityIcon fontSize="small" />
        </IconButton>
      ),
    },
  ];

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Auditoría</Typography>

      <Stack direction="row" spacing={2}>
        <TextField label="Acción" value={accion} onChange={(e) => setAccion(e.target.value)} size="small" />
        <TextField label="Entidad" value={entidad} onChange={(e) => setEntidad(e.target.value)} size="small" />
        <Button variant="contained" onClick={() => setFiltros({ accion, entidad })}>
          Filtrar
        </Button>
      </Stack>

      {isLoading && <CircularProgress />}
      {isError && <Alert severity="error">No se pudo cargar la auditoría.</Alert>}
      {data && <DataTable columns={columnas} rows={data} getRowKey={(a) => a.id} />}

      <Dialog open={!!detalleSeleccionado} onClose={() => setDetalleSeleccionado(null)} fullWidth maxWidth="sm">
        <DialogTitle>Detalle del evento</DialogTitle>
        <DialogContent>
          <pre style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
            {detalleSeleccionado ? formatearDetalle(detalleSeleccionado.detalle) : ''}
          </pre>
        </DialogContent>
      </Dialog>
    </Stack>
  );
}
