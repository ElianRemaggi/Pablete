import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Chip, CircularProgress, Stack, Typography } from '@mui/material';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useSnackbar } from '../../components/SnackbarProvider';
import { extraerMensajeError } from '../../utils/apiError';
import { listarMesasExamen, type MesaExamenResponse } from '../../api/mesasExamenApi';
import {
  darDeBajaInscripcionFinal,
  inscribirFinal,
  listarInscripcionesFinalPropias,
  type InscripcionFinalResponse,
} from '../../api/inscripcionFinalApi';

export function AlumnoInscripcionFinalPage() {
  const queryClient = useQueryClient();
  const { notify } = useSnackbar();
  const [inscripcionABajar, setInscripcionABajar] = useState<InscripcionFinalResponse | null>(null);

  const mesasQuery = useQuery({ queryKey: ['mesas-examen'], queryFn: listarMesasExamen });
  const misInscripcionesQuery = useQuery({
    queryKey: ['inscripciones-final', 'me'],
    queryFn: listarInscripcionesFinalPropias,
  });

  const invalidar = () => queryClient.invalidateQueries({ queryKey: ['inscripciones-final', 'me'] });

  const inscribirMutation = useMutation({
    mutationFn: inscribirFinal,
    onSuccess: () => {
      notify('Inscripción realizada correctamente', 'success');
      invalidar();
    },
    onError: (error) => notify(extraerMensajeError(error, 'No se pudo completar la inscripción'), 'error'),
  });

  const bajaMutation = useMutation({
    mutationFn: darDeBajaInscripcionFinal,
    onSuccess: () => {
      notify('Inscripción dada de baja', 'success');
      invalidar();
      setInscripcionABajar(null);
    },
    onError: (error) => notify(extraerMensajeError(error, 'No se pudo dar de baja la inscripción'), 'error'),
  });

  const columnasMesas: DataTableColumn<MesaExamenResponse>[] = [
    { key: 'materiaNombre', header: 'Materia', render: (m) => m.materiaNombre },
    { key: 'fecha', header: 'Fecha', render: (m) => new Date(m.fecha).toLocaleDateString('es-AR') },
    { key: 'turno', header: 'Turno', render: (m) => m.turno },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (m) => (
        <Button size="small" variant="contained" onClick={() => inscribirMutation.mutate(m.id)}>
          Inscribirme
        </Button>
      ),
    },
  ];

  const columnasInscripciones: DataTableColumn<InscripcionFinalResponse>[] = [
    { key: 'materiaNombre', header: 'Materia', render: (i) => i.materiaNombre },
    { key: 'fechaMesa', header: 'Fecha', render: (i) => new Date(i.fechaMesa).toLocaleDateString('es-AR') },
    { key: 'turno', header: 'Turno', render: (i) => i.turno },
    { key: 'estado', header: 'Estado', render: (i) => <Chip label={i.estado} size="small" /> },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (i) => (
        <Button size="small" color="error" onClick={() => setInscripcionABajar(i)}>
          Dar de baja
        </Button>
      ),
    },
  ];

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Inscripción a final</Typography>

      <Typography variant="h6">Mesas de examen disponibles</Typography>
      {mesasQuery.isLoading && <CircularProgress />}
      {mesasQuery.isError && <Alert severity="error">No se pudieron cargar las mesas de examen.</Alert>}
      {mesasQuery.data && <DataTable columns={columnasMesas} rows={mesasQuery.data} getRowKey={(m) => m.id} />}

      <Typography variant="h6">Mis inscripciones</Typography>
      {misInscripcionesQuery.isLoading && <CircularProgress />}
      {misInscripcionesQuery.isError && <Alert severity="error">No se pudieron cargar tus inscripciones.</Alert>}
      {misInscripcionesQuery.data && (
        <DataTable columns={columnasInscripciones} rows={misInscripcionesQuery.data} getRowKey={(i) => i.id} />
      )}

      <ConfirmDialog
        open={!!inscripcionABajar}
        titulo="Dar de baja inscripción"
        mensaje={`¿Seguro que querés dar de baja tu inscripción a "${inscripcionABajar?.materiaNombre}"?`}
        onConfirm={() => inscripcionABajar && bajaMutation.mutate(inscripcionABajar.id)}
        onCancel={() => setInscripcionABajar(null)}
      />
    </Stack>
  );
}
