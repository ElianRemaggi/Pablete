import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Alert, Button, CircularProgress, Stack, Typography } from '@mui/material';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { EstadoChip } from '../../components/EstadoChip';
import { extraerMensajeError } from '../../utils/apiError';
import { ETIQUETA_TURNO, formatearFecha, formatearHora } from '../../utils/fechas';
import { listarMesasExamen, type MesaExamenResponse } from '../../api/mesasExamenApi';

/** Consulta (solo lectura) de mesas de examen para el preceptor, con acceso al listado de inscriptos. */
export function MesasConsultaPage() {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['mesas-examen', 'preceptor'],
    queryFn: () => listarMesasExamen(),
  });

  const columnas: DataTableColumn<MesaExamenResponse>[] = [
    { key: 'materia', header: 'Materia', render: (m) => m.materiaNombre },
    {
      key: 'fecha',
      header: 'Fecha y hora',
      render: (m) => `${formatearFecha(m.fecha)} ${m.hora ? formatearHora(m.hora) : ''} · ${ETIQUETA_TURNO[m.turno] ?? m.turno}`,
    },
    { key: 'periodo', header: 'Período', render: (m) => m.periodo ?? '—' },
    { key: 'cupo', header: 'Inscriptos / cupo', render: (m) => `${m.inscriptos} / ${m.cupoMaximo}` },
    { key: 'estado', header: 'Estado', render: (m) => <EstadoChip estado={m.estado} /> },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (m) => (
        <Button size="small" component={Link} to={`/preceptor/mesas-examen/${m.id}/inscriptos`}>
          Ver inscriptos
        </Button>
      ),
    },
  ];

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Mesas de examen e inscriptos</Typography>
      {isLoading && <CircularProgress />}
      {isError && <Alert severity="error">{extraerMensajeError(error, 'No se pudieron cargar las mesas.')}</Alert>}
      {data && data.length === 0 && <Alert severity="info">No hay mesas de examen cargadas.</Alert>}
      {data && data.length > 0 && <DataTable columns={columnas} rows={data} getRowKey={(m) => m.id} />}
    </Stack>
  );
}
