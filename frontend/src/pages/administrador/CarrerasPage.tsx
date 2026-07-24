import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Chip, CircularProgress, Stack, TextField, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import IconButton from '@mui/material/IconButton';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { FormModal } from '../../components/FormModal';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useSnackbar } from '../../components/SnackbarProvider';
import {
  actualizarCarrera,
  crearCarrera,
  darDeBajaCarrera,
  listarCarreras,
  type CarreraResponse,
} from '../../api/carrerasApi';

const schema = z.object({
  codigo: z.string().min(1, 'El código es obligatorio'),
  nombre: z.string().min(1, 'El nombre es obligatorio'),
});

type FormValues = z.infer<typeof schema>;

export function CarrerasPage() {
  const queryClient = useQueryClient();
  const { notify } = useSnackbar();
  const { data, isLoading, isError } = useQuery({ queryKey: ['carreras'], queryFn: listarCarreras });

  const [modalAbierto, setModalAbierto] = useState(false);
  const [carreraEnEdicion, setCarreraEnEdicion] = useState<CarreraResponse | null>(null);
  const [carreraABajar, setCarreraABajar] = useState<CarreraResponse | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const invalidar = () => queryClient.invalidateQueries({ queryKey: ['carreras'] });

  const crearMutation = useMutation({
    mutationFn: crearCarrera,
    onSuccess: () => {
      notify('Carrera creada correctamente', 'success');
      invalidar();
      setModalAbierto(false);
    },
    onError: () => notify('No se pudo crear la carrera (¿código duplicado?)', 'error'),
  });

  const actualizarMutation = useMutation({
    mutationFn: ({ id, request }: { id: number; request: FormValues }) => actualizarCarrera(id, request),
    onSuccess: () => {
      notify('Carrera actualizada correctamente', 'success');
      invalidar();
      setModalAbierto(false);
    },
    onError: () => notify('No se pudo actualizar la carrera', 'error'),
  });

  const bajaMutation = useMutation({
    mutationFn: darDeBajaCarrera,
    onSuccess: () => {
      notify('Carrera dada de baja', 'success');
      invalidar();
      setCarreraABajar(null);
    },
    onError: () => notify('No se pudo dar de baja la carrera', 'error'),
  });

  const abrirAlta = () => {
    setCarreraEnEdicion(null);
    reset({ codigo: '', nombre: '' });
    setModalAbierto(true);
  };

  const abrirEdicion = (carrera: CarreraResponse) => {
    setCarreraEnEdicion(carrera);
    reset({ codigo: carrera.codigo, nombre: carrera.nombre });
    setModalAbierto(true);
  };

  const onSubmit = (values: FormValues) => {
    if (carreraEnEdicion) {
      actualizarMutation.mutate({ id: carreraEnEdicion.id, request: values });
    } else {
      crearMutation.mutate(values);
    }
  };

  const columnas: DataTableColumn<CarreraResponse>[] = [
    { key: 'codigo', header: 'Código', render: (c) => c.codigo },
    { key: 'nombre', header: 'Nombre', render: (c) => c.nombre },
    {
      key: 'activa',
      header: 'Estado',
      render: (c) => <Chip label={c.activa ? 'Activa' : 'Baja'} color={c.activa ? 'success' : 'default'} size="small" />,
    },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (c) => (
        <Stack direction="row">
          <IconButton onClick={() => abrirEdicion(c)} size="small">
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton onClick={() => setCarreraABajar(c)} size="small" disabled={!c.activa}>
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Stack>
      ),
    },
  ];

  return (
    <Stack spacing={3}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4">Carreras</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={abrirAlta}>
          Nueva carrera
        </Button>
      </Stack>

      {isLoading && <CircularProgress />}
      {isError && <Alert severity="error">No se pudieron cargar las carreras.</Alert>}
      {data && <DataTable columns={columnas} rows={data} getRowKey={(c) => c.id} />}

      <FormModal
        open={modalAbierto}
        titulo={carreraEnEdicion ? 'Editar carrera' : 'Nueva carrera'}
        onClose={() => setModalAbierto(false)}
        onSubmit={handleSubmit(onSubmit)}
        submitting={crearMutation.isPending || actualizarMutation.isPending}
      >
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField
            label="Código"
            fullWidth
            error={!!errors.codigo}
            helperText={errors.codigo?.message}
            {...register('codigo')}
          />
          <TextField
            label="Nombre"
            fullWidth
            error={!!errors.nombre}
            helperText={errors.nombre?.message}
            {...register('nombre')}
          />
        </Stack>
      </FormModal>

      <ConfirmDialog
        open={!!carreraABajar}
        titulo="Dar de baja carrera"
        mensaje={`¿Seguro que querés dar de baja la carrera "${carreraABajar?.nombre}"?`}
        onConfirm={() => carreraABajar && bajaMutation.mutate(carreraABajar.id)}
        onCancel={() => setCarreraABajar(null)}
      />
    </Stack>
  );
}
