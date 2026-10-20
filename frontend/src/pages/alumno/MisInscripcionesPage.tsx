import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, CircularProgress, Stack, Typography } from '@mui/material';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { EstadoChip } from '../../components/EstadoChip';
import { FileDownloadButton } from '../../components/FileDownloadButton';
import { useSnackbar } from '../../components/SnackbarProvider';
import { extraerMensajeError } from '../../utils/apiError';
import { ETIQUETA_TURNO, formatearFecha, formatearFechaHora, formatearHora } from '../../utils/fechas';
import {
  darDeBajaInscripcionFinal,
  listarInscripcionesFinalPropias,
  type InscripcionFinalResponse,
} from '../../api/inscripcionFinalApi';

export function MisInscripcionesPage() {
  const queryClient = useQueryClient();
  const { notify } = useSnackbar();
  const [inscripcionABajar, setInscripcionABajar] = useState<InscripcionFinalResponse | null>(null);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['inscripciones-final', 'me'],
    queryFn: listarInscripcionesFinalPropias,
  });

  const bajaMutation = useMutation({
    mutationFn: darDeBajaInscripcionFinal,
    onSuccess: () => {
      notify('Inscripción cancelada. Tu comprobante ahora figura como "dada de baja".', 'success');
      queryClient.invalidateQueries({ queryKey: ['inscripciones-final', 'me'] });
      queryClient.invalidateQueries({ queryKey: ['mesas-examen'] });
      setInscripcionABajar(null);
    },
    onError: (err) => {
      notify(extraerMensajeError(err, 'No se pudo cancelar la inscripción'), 'error');
      setInscripcionABajar(null);
    },
  });

  const columnas: DataTableColumn<InscripcionFinalResponse>[] = [
    { key: 'materiaNombre', header: 'Materia', render: (i) => i.materiaNombre },
    { key: 'fechaMesa', header: 'Fecha del examen', render: (i) => `${formatearFecha(i.fechaMesa)} ${formatearHora(i.hora) === '—' ? '' : formatearHora(i.hora)}` },
    { key: 'turno', header: 'Turno', render: (i) => ETIQUETA_TURNO[i.turno] ?? i.turno },
    { key: 'periodo', header: 'Período', render: (i) => i.periodo ?? '—' },
    { key: 'fechaInscripcion', header: 'Inscripto el', render: (i) => formatearFechaHora(i.fechaInscripcion) },
    {
      key: 'estado',
      header: 'Estado',
      render: (i) => (
        <Stack spacing={0.5} sx={{ alignItems: 'flex-start' }}>
          <EstadoChip estado={i.estado} />
          {i.mesaEstado === 'CANCELADA' && (
            <Typography variant="caption" color="error">
              La mesa fue cancelada
            </Typography>
          )}
          {i.resultado && <Typography variant="caption">Resultado: {i.resultado}</Typography>}
        </Stack>
      ),
    },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (i) => (
        <Stack direction="row" spacing={1}>
          <FileDownloadButton
            url={`/certificados/inscripcion-final/${i.id}`}
            nombreArchivo={`comprobante-final-${i.id}.pdf`}
            label="Comprobante PDF"
            size="small"
          />
          {i.estado !== 'BAJA' && (
            <Button size="small" color="error" onClick={() => setInscripcionABajar(i)}>
              Cancelar inscripción
            </Button>
          )}
        </Stack>
      ),
    },
  ];

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Mis inscripciones a finales</Typography>
      <Typography color="text.secondary">
        Incluye el historial: las inscripciones canceladas se conservan y su comprobante refleja el estado real.
        Solo podés cancelar con la antelación mínima que fija la institución.
      </Typography>

      {isLoading && <CircularProgress />}
      {isError && <Alert severity="error">{extraerMensajeError(error, 'No se pudieron cargar tus inscripciones.')}</Alert>}
      {data && data.length === 0 && <Alert severity="info">Todavía no te inscribiste a ninguna mesa de examen.</Alert>}
      {data && data.length > 0 && <DataTable columns={columnas} rows={data} getRowKey={(i) => i.id} />}

      <ConfirmDialog
        open={!!inscripcionABajar}
        titulo="Cancelar inscripción"
        mensaje={`¿Seguro que querés cancelar tu inscripción a "${inscripcionABajar?.materiaNombre}" del ${formatearFecha(inscripcionABajar?.fechaMesa)}?`}
        onConfirm={() => inscripcionABajar && bajaMutation.mutate(inscripcionABajar.id)}
        onCancel={() => setInscripcionABajar(null)}
      />
    </Stack>
  );
}
