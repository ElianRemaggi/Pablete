import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Chip, CircularProgress, Stack, Typography } from '@mui/material';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { FileDownloadButton } from '../../components/FileDownloadButton';
import { useSnackbar } from '../../components/SnackbarProvider';
import { extraerMensajeError } from '../../utils/apiError';
import { listarComisiones, type ComisionResponse } from '../../api/comisionesApi';
import {
  darDeBajaInscripcionCursada,
  inscribirCursada,
  listarInscripcionesCursadaPropias,
  type InscripcionCursadaResponse,
} from '../../api/inscripcionCursadaApi';
import { useState } from 'react';

const DIAS_SEMANA = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

function resumenHorarios(comision: ComisionResponse): string {
  if (comision.horarios.length === 0) return '—';
  return comision.horarios
    .map((h) => `${DIAS_SEMANA[h.diaSemana - 1]} ${h.horaInicio}-${h.horaFin}`)
    .join(', ');
}

export function AlumnoInscripcionCursadaPage() {
  const queryClient = useQueryClient();
  const { notify } = useSnackbar();
  const [inscripcionABajar, setInscripcionABajar] = useState<InscripcionCursadaResponse | null>(null);

  const comisionesQuery = useQuery({ queryKey: ['comisiones'], queryFn: listarComisiones });
  const misInscripcionesQuery = useQuery({
    queryKey: ['inscripciones-cursada', 'me'],
    queryFn: listarInscripcionesCursadaPropias,
  });

  const invalidar = () => {
    queryClient.invalidateQueries({ queryKey: ['inscripciones-cursada', 'me'] });
  };

  const inscribirMutation = useMutation({
    mutationFn: inscribirCursada,
    onSuccess: () => {
      notify('Inscripción realizada correctamente', 'success');
      invalidar();
    },
    onError: (error) => notify(extraerMensajeError(error, 'No se pudo completar la inscripción'), 'error'),
  });

  const bajaMutation = useMutation({
    mutationFn: darDeBajaInscripcionCursada,
    onSuccess: () => {
      notify('Inscripción dada de baja', 'success');
      invalidar();
      setInscripcionABajar(null);
    },
    onError: (error) => notify(extraerMensajeError(error, 'No se pudo dar de baja la inscripción'), 'error'),
  });

  const columnasComisiones: DataTableColumn<ComisionResponse>[] = [
    { key: 'materiaNombre', header: 'Materia', render: (c) => c.materiaNombre },
    { key: 'nombre', header: 'Comisión', render: (c) => c.nombre },
    { key: 'periodoLectivo', header: 'Período', render: (c) => c.periodoLectivo },
    { key: 'horarios', header: 'Horarios', render: resumenHorarios },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (c) => (
        <Button size="small" variant="contained" onClick={() => inscribirMutation.mutate(c.id)}>
          Inscribirme
        </Button>
      ),
    },
  ];

  const columnasInscripciones: DataTableColumn<InscripcionCursadaResponse>[] = [
    { key: 'materiaNombre', header: 'Materia', render: (i) => i.materiaNombre },
    { key: 'comisionNombre', header: 'Comisión', render: (i) => i.comisionNombre },
    { key: 'estado', header: 'Estado', render: (i) => <Chip label={i.estado} size="small" /> },
    {
      key: 'fecha',
      header: 'Fecha de inscripción',
      render: (i) => new Date(i.fechaInscripcion).toLocaleString('es-AR'),
    },
    {
      key: 'comprobante',
      header: 'Comprobante',
      render: (i) => (
        <FileDownloadButton
          url={`/certificados/inscripcion-cursada/${i.id}`}
          nombreArchivo={`comprobante-cursada-${i.id}.pdf`}
          label="PDF"
          size="small"
        />
      ),
    },
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
      <Typography variant="h4">Inscripción a cursada</Typography>

      <Typography variant="h6">Comisiones disponibles</Typography>
      {comisionesQuery.isLoading && <CircularProgress />}
      {comisionesQuery.isError && <Alert severity="error">No se pudieron cargar las comisiones.</Alert>}
      {comisionesQuery.data && (
        <DataTable columns={columnasComisiones} rows={comisionesQuery.data} getRowKey={(c) => c.id} />
      )}

      <Typography variant="h6">Mis inscripciones</Typography>
      {misInscripcionesQuery.isLoading && <CircularProgress />}
      {misInscripcionesQuery.isError && <Alert severity="error">No se pudieron cargar tus inscripciones.</Alert>}
      {misInscripcionesQuery.data && (
        <DataTable columns={columnasInscripciones} rows={misInscripcionesQuery.data} getRowKey={(i) => i.id} />
      )}

      <ConfirmDialog
        open={!!inscripcionABajar}
        titulo="Dar de baja inscripción"
        mensaje={`¿Seguro que querés dar de baja tu inscripción a "${inscripcionABajar?.comisionNombre}"?`}
        onConfirm={() => inscripcionABajar && bajaMutation.mutate(inscripcionABajar.id)}
        onCancel={() => setInscripcionABajar(null)}
      />
    </Stack>
  );
}
