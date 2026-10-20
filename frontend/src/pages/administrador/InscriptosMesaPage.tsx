import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { Alert, Button, Card, CardContent, CircularProgress, Stack, Typography } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { EstadoChip } from '../../components/EstadoChip';
import { FileDownloadButton } from '../../components/FileDownloadButton';
import { extraerMensajeError } from '../../utils/apiError';
import { ETIQUETA_TURNO, formatearFecha, formatearFechaHora, formatearHora } from '../../utils/fechas';
import { obtenerMesaExamen } from '../../api/mesasExamenApi';
import { listarInscriptosPorMesa, type InscripcionFinalResponse } from '../../api/inscripcionFinalApi';

/** Listado de inscriptos de una mesa. Lo comparten administrador y preceptor: el backend autoriza a ambos
 * (InscripcionFinalService.listarPorMesa); `volverA` solo cambia el enlace de regreso segun el panel. */
export function InscriptosMesaPage({ volverA }: { volverA: string }) {
  const { id } = useParams();
  const mesaId = Number(id);

  const mesaQuery = useQuery({ queryKey: ['mesas-examen', mesaId], queryFn: () => obtenerMesaExamen(mesaId) });
  const inscriptosQuery = useQuery({
    queryKey: ['inscripciones-final', 'mesa', mesaId],
    queryFn: () => listarInscriptosPorMesa(mesaId),
  });

  const mesa = mesaQuery.data;
  const activos = (inscriptosQuery.data ?? []).filter((i) => i.estado !== 'BAJA').length;

  const columnas: DataTableColumn<InscripcionFinalResponse>[] = [
    { key: 'apellido', header: 'Apellido y nombre', render: (i) => `${i.alumnoApellido}, ${i.alumnoNombre}` },
    { key: 'dni', header: 'DNI', render: (i) => i.alumnoDni },
    { key: 'legajo', header: 'Legajo', render: (i) => i.alumnoLegajo },
    { key: 'fecha', header: 'Inscripto el', render: (i) => formatearFechaHora(i.fechaInscripcion) },
    { key: 'estado', header: 'Estado', render: (i) => <EstadoChip estado={i.estado} /> },
    {
      key: 'comprobante',
      header: 'Comprobante',
      render: (i) => (
        <FileDownloadButton
          url={`/certificados/alumno/${i.alumnoId}/inscripcion-final/${i.id}`}
          nombreArchivo={`comprobante-final-${i.id}.pdf`}
          label="PDF"
          size="small"
        />
      ),
    },
  ];

  return (
    <Stack spacing={3}>
      <Button component={Link} to={volverA} startIcon={<ArrowBackIcon />} sx={{ alignSelf: 'flex-start' }}>
        Volver a las mesas
      </Button>
      <Typography variant="h4">Inscriptos a la mesa</Typography>

      {mesaQuery.isLoading && <CircularProgress />}
      {mesaQuery.isError && (
        <Alert severity="error">{extraerMensajeError(mesaQuery.error, 'No se pudo cargar la mesa.')}</Alert>
      )}
      {mesa && (
        <Card>
          <CardContent>
            <Stack spacing={0.5}>
              <Typography variant="h6">{mesa.materiaNombre}</Typography>
              <Typography>
                {formatearFecha(mesa.fecha)} {formatearHora(mesa.hora) === '—' ? '' : formatearHora(mesa.hora)} ·{' '}
                {ETIQUETA_TURNO[mesa.turno] ?? mesa.turno} · Período {mesa.periodo ?? '—'} · Aula {mesa.aula ?? '—'}
              </Typography>
              <Typography>
                Cupo: {mesa.inscriptos} inscriptos activos de {mesa.cupoMaximo} ({mesa.cupoDisponible} lugares libres)
              </Typography>
              <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                <Typography>Estado de la mesa:</Typography>
                <EstadoChip estado={mesa.estado} />
              </Stack>
            </Stack>
          </CardContent>
        </Card>
      )}

      <FileDownloadButton
        url="/exportaciones/inscripciones-final"
        params={{ mesaId }}
        nombreArchivo={`inscriptos-mesa-${mesaId}.xlsx`}
        label="Exportar listado a Excel"
        variant="outlined"
        sx={{ alignSelf: 'flex-start' }}
      />

      {inscriptosQuery.isLoading && <CircularProgress />}
      {inscriptosQuery.isError && (
        <Alert severity="error">
          {extraerMensajeError(inscriptosQuery.error, 'No se pudieron cargar los inscriptos.')}
        </Alert>
      )}
      {inscriptosQuery.data && inscriptosQuery.data.length === 0 && (
        <Alert severity="info">Todavía no hay alumnos inscriptos a esta mesa.</Alert>
      )}
      {inscriptosQuery.data && inscriptosQuery.data.length > 0 && (
        <>
          <Typography color="text.secondary">
            {activos} inscripciones activas, {inscriptosQuery.data.length - activos} dadas de baja (se conservan como historial).
          </Typography>
          <DataTable columns={columnas} rows={inscriptosQuery.data} getRowKey={(i) => i.id} />
        </>
      )}
    </Stack>
  );
}
