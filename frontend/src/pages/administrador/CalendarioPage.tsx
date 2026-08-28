import { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, CircularProgress, IconButton, MenuItem, Stack, TextField, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';
import { CalendarioConLeyenda } from '../../components/calendario/CalendarioConLeyenda';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { FormModal } from '../../components/FormModal';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useSnackbar } from '../../components/SnackbarProvider';
import {
  crearEventoAcademico,
  eliminarEventoAcademico,
  listarEventosAcademicos,
  obtenerCalendarioInstitucional,
  type EventoAcademicoResponse,
} from '../../api/calendarioApi';
import { useMesCalendario } from '../../hooks/useMesCalendario';

const schema = z.object({
  titulo: z.string().min(1, 'El título es obligatorio'),
  descripcion: z.string().optional(),
  tipo: z.enum(['EVENTO_ACADEMICO', 'FECHA_LIMITE']),
  fechaInicio: z.string().min(1, 'La fecha de inicio es obligatoria'),
  fechaFin: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

export function AdministradorCalendarioPage() {
  const { mes, desde, hasta, cambiarMes } = useMesCalendario();
  const queryClient = useQueryClient();
  const { notify } = useSnackbar();

  const calendarioQuery = useQuery({
    queryKey: ['calendario', 'institucional', desde.toISOString(), hasta.toISOString()],
    queryFn: () => obtenerCalendarioInstitucional(desde, hasta),
  });
  const eventosQuery = useQuery({ queryKey: ['eventos-academicos'], queryFn: listarEventosAcademicos });

  const [modalAbierto, setModalAbierto] = useState(false);
  const [eventoAEliminar, setEventoAEliminar] = useState<EventoAcademicoResponse | null>(null);

  const {
    control,
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const invalidar = () => {
    queryClient.invalidateQueries({ queryKey: ['eventos-academicos'] });
    queryClient.invalidateQueries({ queryKey: ['calendario'] });
  };

  const crearMutation = useMutation({
    mutationFn: crearEventoAcademico,
    onSuccess: () => {
      notify('Evento académico creado correctamente', 'success');
      invalidar();
      setModalAbierto(false);
    },
    onError: () => notify('No se pudo crear el evento académico', 'error'),
  });

  const eliminarMutation = useMutation({
    mutationFn: eliminarEventoAcademico,
    onSuccess: () => {
      notify('Evento académico eliminado', 'success');
      invalidar();
      setEventoAEliminar(null);
    },
    onError: () => notify('No se pudo eliminar el evento académico', 'error'),
  });

  const abrirAlta = () => {
    reset({ titulo: '', descripcion: '', tipo: 'EVENTO_ACADEMICO', fechaInicio: '', fechaFin: '' });
    setModalAbierto(true);
  };

  const columnas: DataTableColumn<EventoAcademicoResponse>[] = [
    { key: 'titulo', header: 'Título', render: (e) => e.titulo },
    { key: 'tipo', header: 'Tipo', render: (e) => (e.tipo === 'FECHA_LIMITE' ? 'Fecha límite' : 'Evento académico') },
    { key: 'fechaInicio', header: 'Inicio', render: (e) => new Date(e.fechaInicio).toLocaleString('es-AR') },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (e) => (
        <IconButton onClick={() => setEventoAEliminar(e)} size="small">
          <DeleteIcon fontSize="small" />
        </IconButton>
      ),
    },
  ];

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Calendario institucional</Typography>
      <CalendarioConLeyenda
        mes={mes}
        eventos={calendarioQuery.data}
        isLoading={calendarioQuery.isLoading}
        isError={calendarioQuery.isError}
        onCambiarMes={cambiarMes}
      />

      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h5">Eventos académicos</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={abrirAlta}>
          Nuevo evento
        </Button>
      </Stack>

      {eventosQuery.isLoading && <CircularProgress />}
      {eventosQuery.isError && <Alert severity="error">No se pudieron cargar los eventos académicos.</Alert>}
      {eventosQuery.data && eventosQuery.data.length === 0 && (
        <Alert severity="info">No hay eventos académicos cargados todavía.</Alert>
      )}
      {eventosQuery.data && eventosQuery.data.length > 0 && (
        <DataTable columns={columnas} rows={eventosQuery.data} getRowKey={(e) => e.id} />
      )}

      <FormModal
        open={modalAbierto}
        titulo="Nuevo evento académico"
        onClose={() => setModalAbierto(false)}
        onSubmit={handleSubmit((values) => crearMutation.mutate(values))}
        submitting={crearMutation.isPending}
      >
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField
            label="Título"
            fullWidth
            error={!!errors.titulo}
            helperText={errors.titulo?.message}
            {...register('titulo')}
          />
          <TextField label="Descripción" fullWidth multiline minRows={2} {...register('descripcion')} />
          <Controller
            name="tipo"
            control={control}
            render={({ field }) => (
              <TextField select label="Tipo" fullWidth {...field}>
                <MenuItem value="EVENTO_ACADEMICO">Evento académico</MenuItem>
                <MenuItem value="FECHA_LIMITE">Fecha límite</MenuItem>
              </TextField>
            )}
          />
          <TextField
            label="Fecha y hora de inicio"
            type="datetime-local"
            fullWidth
            slotProps={{ inputLabel: { shrink: true } }}
            error={!!errors.fechaInicio}
            helperText={errors.fechaInicio?.message}
            {...register('fechaInicio')}
          />
          <TextField
            label="Fecha y hora de fin (opcional)"
            type="datetime-local"
            fullWidth
            slotProps={{ inputLabel: { shrink: true } }}
            {...register('fechaFin')}
          />
        </Stack>
      </FormModal>

      <ConfirmDialog
        open={!!eventoAEliminar}
        titulo="Eliminar evento académico"
        mensaje={`¿Seguro que querés eliminar "${eventoAEliminar?.titulo}"?`}
        onConfirm={() => eventoAEliminar && eliminarMutation.mutate(eventoAEliminar.id)}
        onCancel={() => setEventoAEliminar(null)}
      />
    </Stack>
  );
}
