import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Chip, CircularProgress, IconButton, Stack, TextField, Typography, Button } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { DataTable, type DataTableColumn } from '../../components/DataTable';
import { FormModal } from '../../components/FormModal';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { useSnackbar } from '../../components/SnackbarProvider';
import {
  actualizarMateria,
  crearMateria,
  darDeBajaMateria,
  listarMaterias,
  type MateriaResponse,
} from '../../api/materiasApi';

const schema = z.object({
  codigo: z.string().min(1, 'El código es obligatorio'),
  nombre: z.string().min(1, 'El nombre es obligatorio'),
  cargaHoraria: z.coerce.number().int().min(1, 'Debe ser mayor a 0'),
});

type FormInput = z.input<typeof schema>;
type FormValues = z.output<typeof schema>;

export function MateriasPage() {
  const queryClient = useQueryClient();
  const { notify } = useSnackbar();
  const { data, isLoading, isError } = useQuery({ queryKey: ['materias'], queryFn: listarMaterias });

  const [modalAbierto, setModalAbierto] = useState(false);
  const [materiaEnEdicion, setMateriaEnEdicion] = useState<MateriaResponse | null>(null);
  const [materiaABajar, setMateriaABajar] = useState<MateriaResponse | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormInput, unknown, FormValues>({ resolver: zodResolver(schema) });

  const invalidar = () => queryClient.invalidateQueries({ queryKey: ['materias'] });

  const crearMutation = useMutation({
    mutationFn: crearMateria,
    onSuccess: () => {
      notify('Materia creada correctamente', 'success');
      invalidar();
      setModalAbierto(false);
    },
    onError: () => notify('No se pudo crear la materia (¿código duplicado?)', 'error'),
  });

  const actualizarMutation = useMutation({
    mutationFn: ({ id, request }: { id: number; request: FormValues }) => actualizarMateria(id, request),
    onSuccess: () => {
      notify('Materia actualizada correctamente', 'success');
      invalidar();
      setModalAbierto(false);
    },
    onError: () => notify('No se pudo actualizar la materia', 'error'),
  });

  const bajaMutation = useMutation({
    mutationFn: darDeBajaMateria,
    onSuccess: () => {
      notify('Materia dada de baja', 'success');
      invalidar();
      setMateriaABajar(null);
    },
    onError: () => notify('No se pudo dar de baja la materia', 'error'),
  });

  const abrirAlta = () => {
    setMateriaEnEdicion(null);
    reset({ codigo: '', nombre: '', cargaHoraria: 0 });
    setModalAbierto(true);
  };

  const abrirEdicion = (materia: MateriaResponse) => {
    setMateriaEnEdicion(materia);
    reset({ codigo: materia.codigo, nombre: materia.nombre, cargaHoraria: materia.cargaHoraria });
    setModalAbierto(true);
  };

  const onSubmit = (values: FormValues) => {
    if (materiaEnEdicion) {
      actualizarMutation.mutate({ id: materiaEnEdicion.id, request: values });
    } else {
      crearMutation.mutate(values);
    }
  };

  const columnas: DataTableColumn<MateriaResponse>[] = [
    { key: 'codigo', header: 'Código', render: (m) => m.codigo },
    { key: 'nombre', header: 'Nombre', render: (m) => m.nombre },
    { key: 'cargaHoraria', header: 'Carga horaria', render: (m) => m.cargaHoraria },
    {
      key: 'activa',
      header: 'Estado',
      render: (m) => (
        <Chip label={m.activa ? 'Activa' : 'Baja'} color={m.activa ? 'success' : 'default'} size="small" />
      ),
    },
    {
      key: 'acciones',
      header: 'Acciones',
      render: (m) => (
        <Stack direction="row">
          <IconButton onClick={() => abrirEdicion(m)} size="small">
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton onClick={() => setMateriaABajar(m)} size="small" disabled={!m.activa}>
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Stack>
      ),
    },
  ];

  return (
    <Stack spacing={3}>
      <Stack direction="row" sx={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4">Materias</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={abrirAlta}>
          Nueva materia
        </Button>
      </Stack>

      {isLoading && <CircularProgress />}
      {isError && <Alert severity="error">No se pudieron cargar las materias.</Alert>}
      {data && <DataTable columns={columnas} rows={data} getRowKey={(m) => m.id} />}

      <FormModal
        open={modalAbierto}
        titulo={materiaEnEdicion ? 'Editar materia' : 'Nueva materia'}
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
          <TextField
            label="Carga horaria"
            type="number"
            fullWidth
            error={!!errors.cargaHoraria}
            helperText={errors.cargaHoraria?.message}
            {...register('cargaHoraria')}
          />
        </Stack>
      </FormModal>

      <ConfirmDialog
        open={!!materiaABajar}
        titulo="Dar de baja materia"
        mensaje={`¿Seguro que querés dar de baja la materia "${materiaABajar?.nombre}"?`}
        onConfirm={() => materiaABajar && bajaMutation.mutate(materiaABajar.id)}
        onCancel={() => setMateriaABajar(null)}
      />
    </Stack>
  );
}
