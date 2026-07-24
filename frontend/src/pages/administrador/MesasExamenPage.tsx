import { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, CircularProgress, IconButton, MenuItem, Stack, TextField, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { FormModal } from '../../components/FormModal';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useSnackbar } from '../../components/SnackbarProvider';
import { listarMaterias } from '../../api/materiasApi';
import {
  actualizarMesaExamen,
  crearMesaExamen,
  eliminarMesaExamen,
  listarMesasExamen,
  type MesaExamenResponse,
} from '../../api/mesasExamenApi';

const schema = z.object({
  materiaId: z.coerce.number().int().positive('Elegí una materia'),
  fecha: z.string().min(1, 'La fecha es obligatoria'),
  turno: z.enum(['MANANA', 'TARDE', 'NOCHE']),
  cupoMaximo: z.coerce.number().int().min(1, 'Debe ser mayor a 0'),
});

type FormInput = z.input<typeof schema>;
type FormValues = z.output<typeof schema>;

export function MesasExamenPage() {
  const queryClient = useQueryClient();
  const { notify } = useSnackbar();
  const materiasQuery = useQuery({ queryKey: ['materias'], queryFn: listarMaterias });
  const { data, isLoading, isError } = useQuery({ queryKey: ['mesas-examen'], queryFn: listarMesasExamen });

  const [modalAbierto, setModalAbierto] = useState(false);
  const [mesaEnEdicion, setMesaEnEdicion] = useState<MesaExamenResponse | null>(null);
  const [mesaAEliminar, setMesaAEliminar] = useState<MesaExamenResponse | null>(null);

  const {
    control,
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormInput, unknown, FormValues>({ resolver: zodResolver(schema) });

  const invalidar = () => queryClient.invalidateQueries({ queryKey: ['mesas-examen'] });

  const crearMutation = useMutation({
    mutationFn: crearMesaExamen,
    onSuccess: () => {
      notify('Mesa de examen creada correctamente', 'success');
      invalidar();
      setModalAbierto(false);
    },
    onError: () => notify('No se pudo crear la mesa (¿duplicada o fuera de fecha?)', 'error'),
  });

  const actualizarMutation = useMutation({
    mutationFn: ({ id, request }: { id: number; request: FormValues }) => actualizarMesaExamen(id, request),
    onSuccess: () => {
      notify('Mesa de examen actualizada correctamente', 'success');
      invalidar();
      setModalAbierto(false);
    },
    onError: () => notify('No se pudo actualizar la mesa', 'error'),
  });

  const eliminarMutation = useMutation({
    mutationFn: eliminarMesaExamen,
    onSuccess: () => {
      notify('Mesa de examen eliminada', 'success');
      invalidar();
      setMesaAEliminar(null);
    },
    onError: () => notify('No se pudo eliminar la mesa', 'error'),
  });

  const abrirAlta = () => {
    setMesaEnEdicion(null);
    reset({ materiaId: undefined, fecha: '', turno: 'MANANA', cupoMaximo: 0 });
    setModalAbierto(true);
  };

  const abrirEdicion = (mesa: MesaExamenResponse) => {
    setMesaEnEdicion(mesa);
    reset({ materiaId: mesa.materiaId, fecha: mesa.fecha, turno: mesa.turno as FormValues['turno'], cupoMaximo: mesa.cupoMaximo });
    setModalAbierto(true);
  };

  const onSubmit = (values: FormValues) => {
    if (mesaEnEdicion) {
      actualizarMutation.mutate({ id: mesaEnEdicion.id, request: values });
    } else {
      crearMutation.mutate(values);
    }
  };

  const columnas: DataTableColumn<MesaExamenResponse>[] = [
    { key: 'materiaNombre', header: 'Materia', render: (m) => m.materiaNombre },
    { key: 'fecha', header: 'Fecha', render: (m) => new Date(m.fecha).toLocaleDateString('es-AR') },
    { key: 'turno', header: 'Turno', render: (m) => m.turno },
    { key: 'cupoMaximo', header: 'Cupo', render: (m) => m.cupoMaximo },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (m) => (
        <Stack direction="row">
          <IconButton onClick={() => abrirEdicion(m)} size="small">
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton onClick={() => setMesaAEliminar(m)} size="small">
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Stack>
      ),
    },
  ];

  return (
    <Stack spacing={3}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4">Mesas de examen</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={abrirAlta}>
          Nueva mesa
        </Button>
      </Stack>

      {isLoading && <CircularProgress />}
      {isError && <Alert severity="error">No se pudieron cargar las mesas de examen.</Alert>}
      {data && <DataTable columns={columnas} rows={data} getRowKey={(m) => m.id} />}

      <FormModal
        open={modalAbierto}
        titulo={mesaEnEdicion ? 'Editar mesa de examen' : 'Nueva mesa de examen'}
        onClose={() => setModalAbierto(false)}
        onSubmit={handleSubmit(onSubmit)}
        submitting={crearMutation.isPending || actualizarMutation.isPending}
      >
        <Stack spacing={2} sx={{ mt: 1 }}>
          <Controller
            name="materiaId"
            control={control}
            render={({ field }) => (
              <TextField
                select
                label="Materia"
                fullWidth
                error={!!errors.materiaId}
                helperText={errors.materiaId?.message}
                {...field}
              >
                {(materiasQuery.data ?? []).map((materia) => (
                  <MenuItem key={materia.id} value={materia.id}>
                    {materia.codigo} — {materia.nombre}
                  </MenuItem>
                ))}
              </TextField>
            )}
          />
          <TextField
            label="Fecha"
            type="date"
            fullWidth
            slotProps={{ inputLabel: { shrink: true } }}
            error={!!errors.fecha}
            helperText={errors.fecha?.message}
            {...register('fecha')}
          />
          <Controller
            name="turno"
            control={control}
            render={({ field }) => (
              <TextField select label="Turno" fullWidth {...field}>
                <MenuItem value="MANANA">Mañana</MenuItem>
                <MenuItem value="TARDE">Tarde</MenuItem>
                <MenuItem value="NOCHE">Noche</MenuItem>
              </TextField>
            )}
          />
          <TextField
            label="Cupo máximo"
            type="number"
            fullWidth
            error={!!errors.cupoMaximo}
            helperText={errors.cupoMaximo?.message}
            {...register('cupoMaximo')}
          />
        </Stack>
      </FormModal>

      <ConfirmDialog
        open={!!mesaAEliminar}
        titulo="Eliminar mesa de examen"
        mensaje={`¿Seguro que querés eliminar la mesa de "${mesaAEliminar?.materiaNombre}"?`}
        onConfirm={() => mesaAEliminar && eliminarMutation.mutate(mesaAEliminar.id)}
        onCancel={() => setMesaAEliminar(null)}
      />
    </Stack>
  );
}
